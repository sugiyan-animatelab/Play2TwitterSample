package controllers

import com.sun.xml.internal.bind.v2.TODO
import play.api._
import libs.oauth._
import libs.oauth.ConsumerKey
import libs.oauth.OAuth
import libs.oauth.RequestToken
import libs.oauth.ServiceInfo
import libs.ws.WS
import play.api.mvc._
import scala.Left
import scala.Right
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global

//import scala.swing.Action

class Application extends Controller {

  val consumerKey = "X4VQq5TQ0ABmgqknJapKWY2gW"
  val consumerSecret = "qXMQEA4d12qpHjepG7N8qhRIQxFpBIQOSDv8yoKKxDEPOaZYVw"

  def index = Action.async {
    //Ok(views.html.index("Your new application is ready."))
    request => {

      //セッションの中身の確認
      val accessToken = request.session.get("token").map {
        token =>
          println("token : " + token)
          token
      }.getOrElse {
        println("not token get")
        ""
      }
      val accessTokenSecret = request.session.get("secret").map {
        secret =>
          println("secret : " + secret)
          secret
      }.getOrElse {
        println("not secret get")
        ""
      }

      //ログイン判定
      if (!(accessToken == "" && accessTokenSecret == "")) {
        println("rogin")
      } else {
        println("rogout")
      }

      //認証情報の作成
      val oauthCalculator = OAuthCalculator(ConsumerKey(consumerKey, consumerSecret), RequestToken(accessToken, accessTokenSecret))

      //タイムラインの取得
      val url = "https://api.twitter.com/1.1/statuses/home_timeline.json"
//      Action.async {
        WS.url(url).sign(oauthCalculator).get().map {
          response =>
            Ok(views.html.index(response.body))
        }
//      }

    }

  }

//  def authenticate = TODO

//  def logout = TODO
  def logout = Action {
    Redirect(routes.Application.index).withNewSession
  }

  /*
   * twitter認証系のテスト
   */
  val KEY = ConsumerKey(consumerKey, consumerSecret)

  val TWITTER = OAuth(ServiceInfo(
    "https://api.twitter.com/oauth/request_token",
    "https://api.twitter.com/oauth/access_token",
    "https://api.twitter.com/oauth/authorize", KEY),
    false)

  def authenticate = Action {
    request =>
      request.queryString.get("oauth_verifier").flatMap(_.headOption).map {
        verifier =>
          val tokenPair = sessionTokenPair(request).get
          // We got the verifier; now get the access token, store it and back to index
          println("認証されました。アクセストークンを取得し、保存し、indexに戻ります")
          TWITTER.retrieveAccessToken(tokenPair, verifier) match {
            case Right(t) => {
              // We received the authorized tokens in the OAuth object - store it before we proceed
              println("Oauthオブジェクトからアクセストークンを受け取りました。それを保存します。")
              Redirect(routes.Application.index).withSession("token" -> t.token, "secret" -> t.secret)
            }
            case Left(e) => throw e
          }
      }.getOrElse(
        TWITTER.retrieveRequestToken("http://localhost:9000/auth") match {
          //コールバックURL
          case Right(t) => {
            // We received the unauthorized tokens in the OAuth object - store it before we proceed
            println("認証されてないトークンを受け取りました。それを保存します。")
            Redirect(TWITTER.redirectUrl(t.token)).withSession("token" -> t.token, "secret" -> t.secret)
          }
          case Left(e) => throw e
        })
  }

  def sessionTokenPair(implicit request: RequestHeader): Option[RequestToken] = {
    for {
      token <- request.session.get("token")
      secret <- request.session.get("secret")
    } yield {
      RequestToken(token, secret)
    }
  }

}