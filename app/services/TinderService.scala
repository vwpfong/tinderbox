package services

import java.util.concurrent.ConcurrentNavigableMap

import scala.collection.mutable.Map
import scala.concurrent._
import scala.concurrent.duration._
import org.mapdb._
import utils.tinder.TinderApi
import utils.tinder.model._

/**
 * Manages state in-memory of sessions for Tinder.
 */
object TinderService {

  /**
   * Current active tokens.
   */
  private val sessions: ConcurrentNavigableMap[String, TinderAuth] = MapDB.db.getTreeMap("sessions")

  /**
   * Retrieve an active session.
   * @param xAuthToken
   * @return
   */
  def fetchSession(xAuthToken: String): Option[TinderAuth] = {
    sessions.get(xAuthToken) match {
      case null =>
        val tinderApi = new TinderApi(Some(xAuthToken))
        val result = Await.result(tinderApi.getProfile, 10 seconds)
        result match {
          case Left(error) => None
          case Right(profile) =>
            // create a placeholder auth object
            val tinderAuth = new TinderAuth(xAuthToken,new TinderGlobals,profile,new TinderVersion)
            // save it
            storeSession(tinderAuth)
            Some(tinderAuth)
        }
      case session => Some(session)
    }
  }

  /**
   * Save an active session asynchronously.
   * @param tinderAuth
   */
  def storeSession(tinderAuth: TinderAuth) { sessions.put(tinderAuth.token, tinderAuth) }

  /**
   * End an active session asynchronously.
   * @param xAuthToken
   */
  def deleteSession(xAuthToken: String) { sessions.remove(xAuthToken) }

}