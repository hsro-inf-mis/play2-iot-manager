package controllers

import models.api._
import org.scalatestplus.play.{BaseOneAppPerSuite, PlaySpec}
import play.api.libs.json._
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import play.api.libs.json.Reads
import models.api.PagedGen._
import scala.concurrent.Future

/**
  * Runs a functional test with the application, using a test PostgreSql
  * database.  Migrations are handled automatically by flyway
  */
class OwnerControllerTest extends PlaySpec with BaseOneAppPerSuite with TestApplicationFactory {

  /** ——————————————————— */
  /** OWNER CONTROLLER */
  /** ——————————————————— */

  "ContactController" should {
    "accept and execute POST request" in {
      val future: Future[Result] = route(app, FakeRequest(POST, "/v1/owners").withJsonBody(Json.parse(
        s"""{
           |  "firstName": "John",
           |  "lastName": "Schneider"
           |}""".stripMargin))).get
      status(future) mustEqual 200
      contentType(future) mustBe Some("application/json")
      val content = contentAsJson(future)
      val ParsedObject = content.as[OwnerDTO]
      ParsedObject.firstName mustEqual Some("John")
      ParsedObject.lastName mustEqual Some("Schneider")
      ParsedObject.city mustEqual None
    }
    "accept and execute GET request" in {
      val future: Future[Result] = route(app, FakeRequest(GET, "/v1/owners/3")).get
      status(future) mustEqual 200
      val notFound: Future[Result] = route(app, FakeRequest(GET, "/v1/owners/100")).get
      status(notFound) mustEqual 404
      contentType(future) mustBe Some("application/json")
      val content = contentAsJson(future)
      val ParsedObject = content.as[OwnerDTO]
      ParsedObject.firstName mustEqual Some("John")
      ParsedObject.lastName mustEqual Some("Schneider")
      ParsedObject.city mustEqual None
    }
    "accept and execute PUT request" in {
      val future: Future[Result] = route(app, FakeRequest(PUT, "/v1/owners").withJsonBody(Json.parse(
        s"""{
           |  "id": 1,
           |  "firstName": "Tom"
           |}""".stripMargin))).get
      status(future) mustEqual 204
      val notFound: Future[Result] = route(app, FakeRequest(PUT, "/v1/owners").withJsonBody(Json.parse(
        s"""{
           |  "id": 100,
           |  "firstName": "Tom",
           |  "lastName": "Schneider"
           |}""".stripMargin))).get
      status(notFound) mustEqual 404
      val getAfterPut = route(app, FakeRequest(GET, "/v1/owners/1")).get
      val content = contentAsJson(getAfterPut)
      val ParsedObject = content.as[OwnerDTO]
      ParsedObject.lastName mustEqual None
      ParsedObject.firstName mustEqual Some("Tom")
      ParsedObject.city mustEqual None
    }
    "accept and execute PATCH request" in {
      val future: Future[Result] = route(app, FakeRequest(PATCH, "/v1/owners").withJsonBody(Json.parse(
        s"""{
           |  "id": 1,
           |  "lastName": "Schneider",
           |  "city": "TestCity"
           |}""".stripMargin))).get
      status(future) mustEqual 204
      val notFound: Future[Result] = route(app, FakeRequest(PATCH, "/v1/owners").withJsonBody(Json.parse(
        s"""{
           |  "id": 100,
           |  "firstName": "Tom",
           |  "city": "TestCity"
           |}""".stripMargin))).get
      status(notFound) mustEqual 404
      val getAfterPatch = route(app, FakeRequest(GET, "/v1/owners/1")).get
      val content = contentAsJson(getAfterPatch)
      val ParsedObject = content.as[OwnerDTO]
      ParsedObject.firstName mustEqual Some("Tom")
      ParsedObject.lastName mustEqual Some("Schneider")
      ParsedObject.city mustEqual Some("TestCity")
      ParsedObject.changedDate mustBe a [Some[_]]
    }
    "accept and execute DELETE request" in {
      val future: Future[Result] = route(app, FakeRequest(DELETE, "/v1/owners/1")).get
      status(future) mustEqual 204
      val futureIsDeleted: Future[Result] = route(app, FakeRequest(DELETE, "/v1/owners/1")).get
      status(futureIsDeleted) mustEqual 500
      val notFound = route(app, FakeRequest(DELETE, "/v1/owners/100")).get
      status(notFound) mustEqual 404
      val futureAfterDelete: Future[Result] = route(app, FakeRequest(GET, "/v1/owners/1")).get
      status(futureAfterDelete) mustEqual 404
      val getAfterDelete = route(app, FakeRequest(GET, "/v1/owners/2?showDeleted=true")).get
      val content = contentAsJson(getAfterDelete)
      val ParsedObject = content.as[OwnerDTO]
      ParsedObject.firstName mustEqual Some("Testname2")
      ParsedObject.city mustEqual Some("testCity2")
      ParsedObject.lastName mustEqual None
      ParsedObject.changedDate mustBe None
    }
    "accept and execute GET ALL request" in {
      val future: Future[Result] = route(app, FakeRequest(GET, "/v1/owners?startIndex=1&endIndex=1&showDeleted=true")).get
      val futureAll: Future[Result] = route(app, FakeRequest(GET, "/v1/owners?startIndex=0&endIndex=300")).get
      status(future) mustEqual 200
      status(future) mustEqual 200
      contentType(futureAll) mustBe Some("application/json")
      contentType(future) mustBe Some("application/json")
      val content = contentAsJson(future)
      val ParsedObject = content.as[PagedData[JsValue]]
      val contentAll = contentAsJson(futureAll)
      val ParsedAll = contentAll.as[PagedData[JsValue]]
      ParsedObject.count mustEqual 3
      ParsedObject.prev mustEqual "/v1/owners?startIndex=0&endIndex=0"
      ParsedObject.next mustEqual "/v1/owners?startIndex=2&endIndex=2"
      ParsedAll.next mustEqual ""
      ParsedAll.prev mustEqual ""
      ParsedAll.count mustEqual 2
    }
  }
}
