package play.api.libs.ws

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.mockito.Mockito._
import org.mockito.internal.creation.MockSettingsImpl
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.specs2.matcher.MustMatchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class RememberedHeadersFilterTest extends Specification with MustMatchers with Mockito with AfterAll {

  // Create Akka system for thread and streaming management
  implicit val system = ActorSystem()
  private implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()
  // Create the standalone WS client
  val client = StandaloneAhcWSClient()

  private val portNumber = 0
  private val emptyHeaders = Map.empty[String, Seq[String]]
  private val testUrl = s"http://localhost:$portNumber/any/test/uri"
  private val testKey = testUrl
  private val testETagValue = "testETagValue"
  private val responseHeadersWithETag = Map("ETag" -> Seq(testETagValue)) // TODO: Change after moving of StandardValues.scala to Map(ETAG -> Seq(testETagValue))
  private val requestHeadersWithETag = Map("If-None-Match" -> Seq(testETagValue)) // TODO: Change after moving of StandardValues.scala to Map(IF_NONE_MATCH -> Seq(testETagValue))

  private val defaultTimeout = 5.seconds
  private val defaultAnswer = new MockSettingsImpl().defaultAnswer(new Answer[Map[String, Seq[String]]] { // TODO: Remove after drop support of Scala-2.11
    override def answer(invocation: InvocationOnMock): Map[String, Seq[String]] = emptyHeaders
  })

  override def afterAll: Unit = {
    client.close()
    val terminate = system.terminate()
    Await.ready(terminate, defaultTimeout)
  }

  "A Headers WSFilter" should {

    "leave request unmodified if there are no header value at cache" in {
      val request = client.url(testUrl)

      val mockExecutor = mock[WSRequestExecutor]
      theStubbed(mockExecutor.apply(any[StandaloneWSRequest])) answers {
        actual => {
          actual  mustEqual request  // The main check
  
          val response = mock[StandaloneWSResponse]
          when(response.status) thenReturn 200 // TODO: Change after moving of StandardValues.scala to Status.OK
          when(response.headers) thenReturn emptyHeaders
  
          Future.successful(response)
      }}

      val filter = new RememberedHeadersFilter(mock[RememberedHeaders](defaultAnswer))
      Await.ready(filter(mockExecutor).apply(request), defaultTimeout)
      ok
    }

    "not put anything to storage if WSResponse does not contain headers" in {
      val headers = mock[RememberedHeaders](defaultAnswer)

      val mockExecutor = mock[WSRequestExecutor]
      theStubbed(mockExecutor.apply(any[StandaloneWSRequest])) answers { _ => {

        val response = mock[StandaloneWSResponse]
        when(response.status) thenReturn 200 // TODO: Change after moving of StandardValues.scala to Status.OK
        when(response.headers) thenReturn emptyHeaders

        Future.successful(response)
      }}

      val filter = new RememberedHeadersFilter(headers)
      Await.ready(filter(mockExecutor).apply(client.url(testUrl)), defaultTimeout)

      verify(headers, never()).set(anyString, any) // The main check
      ok
    }

    "add headers to StandaloneWSRequest if got any from storage" in {
      val headers = mock[RememberedHeaders](defaultAnswer)
      when(headers.get(testKey)) thenReturn requestHeadersWithETag // Main precondition

      val request = client.url(testUrl)

      val mockExecutor = mock[WSRequestExecutor]
      theStubbed(mockExecutor.apply(any[StandaloneWSRequest])) answers { actual => {
        actual.asInstanceOf[StandaloneWSRequest].header("If-None-Match") must beSome(testETagValue) // The main check // TODO: Change after moving of StandardValues.scala to actual.asInstanceOf[StandaloneWSRequest].header(IF_NONE_MATCH) must beSome(testETagValue)

        val response = mock[StandaloneWSResponse]
        when(response.status) thenReturn 200 // TODO: Change after moving of StandardValues.scala to Status.OK
        when(response.headers) thenReturn emptyHeaders

        Future.successful(response)
      }}

      val filter = new RememberedHeadersFilter(headers)
      Await.ready(filter(mockExecutor).apply(request), defaultTimeout)
      ok
    }

    "store headers if any header is present at WSResponse" in {
      val headers = mock[RememberedHeaders]
      when(headers.get(testKey)) thenReturn emptyHeaders

      val mockExecutor = mock[WSRequestExecutor]
      theStubbed(mockExecutor.apply(any[StandaloneWSRequest])) answers { _ => {

        val response = mock[StandaloneWSResponse]
        when(response.status) thenReturn 200 // TODO: Change after moving of StandardValues.scala to Status.OK
        when(response.headers) thenReturn responseHeadersWithETag // Main precondition

        Future.successful(response)
      }}

      val filter = new RememberedHeadersFilter(headers)
      Await.ready(filter(mockExecutor).apply(client.url(testUrl)), defaultTimeout)

      // Cheating with timeouts because of usage of onComplete callback on completed Future. Await.ready only or single threaded executor just don't work.
      verify(headers, timeout(100).times(1)).set(testKey, responseHeadersWithETag) // The main check
      ok
    }

    "not store headers if WSResponse wasn't Ok" in {
      val headers = mock[RememberedHeaders](defaultAnswer)

      val mockExecutor = mock[WSRequestExecutor]

      theStubbed(mockExecutor.apply(any[StandaloneWSRequest])) answers { _ => {

        val response = mock[StandaloneWSResponse]
        when(response.status) thenReturn 304 // TODO: Change after moving of StandardValues.scala to Status.NOT_MODIFIED
        when(response.headers) thenReturn responseHeadersWithETag // Main precondition

        Future.successful(response)
      }}

      val filter = new RememberedHeadersFilter(headers)
      Await.ready(filter(mockExecutor).apply(client.url(testUrl)), defaultTimeout)

      verify(headers, never()).set(anyString, any) // The main check
      ok
    }

    "not store headers if interaction wasn't completed successfully" in {
      val headers = mock[RememberedHeaders]
      when(headers.get(testKey)) thenReturn emptyHeaders

      val mockExecutor = mock[WSRequestExecutor]
      theStubbed(mockExecutor.apply(any[StandaloneWSRequest])) answers { _ => {
        Future.failed(new RuntimeException) // Main precondition
      }}

      val filter = new RememberedHeadersFilter(headers)
      Await.result(filter(mockExecutor).apply(client.url(testUrl)), defaultTimeout) must throwA[RuntimeException]

      verify(headers, never()).set(anyString, any) // The main check
      ok
    }
  }
}