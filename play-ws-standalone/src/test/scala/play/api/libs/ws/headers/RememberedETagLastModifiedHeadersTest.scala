package play.api.libs.ws.headers

import org.mockito.Mockito._
import org.specs2.matcher.MustMatchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.libs.ws._

class RememberedETagLastModifiedHeadersTest extends Specification with MustMatchers with Mockito {

  private val testKey = "someTestKey"
  private val emptyReply = Map.empty[String, Seq[String]]

  "ETagLastModifiedHeadersTest" should {

    // TODO: Change after moving of StandardValues.scala to s"not put anything further if candidate value does not contain neither $ETAG nor $LAST_MODIFIED values" in {
    "not put anything further if candidate value does not contain neither ETag nor Last-Modified values" in {
      val headers = mock[RememberedHeaders]
      val testValue = Map("foo" -> Seq("bar"))
      val target = new RememberedEtagLastModifiedHeaders(headers)

      target.set(testKey, testValue)

      verify(headers, never()).set(anyString, any[Map[String, Seq[String]]])
      ok
    }

    // TODO: Change after moving of StandardValues.scala to s"get empty reply if headers not contain neither $ETAG nor $LAST_MODIFIED values" in {
    "get empty reply if headers not contain neither ETag nor Last-Modified values" in {
      val headers = mock[RememberedHeaders] //(answer({ _ => emptyReply }))
      val testValue = Map("foo" -> Seq("bar"))
      when(headers.get(testKey)) thenReturn testValue

      val target = new RememberedEtagLastModifiedHeaders(headers)
      target.get(testKey) must be(emptyReply)
    }

    // TODO: Change after moving of StandardValues.scala to s"prepare $IF_NONE_MATCH header if headers contain $ETAG value" in {
    "prepare If-None-Match header if headers contain ETag value" in {
      val headers = mock[RememberedHeaders] //(answer({ _ => emptyReply }))
      val testValue = Map("ETag" -> Seq("bar")) // TODO: Change after moving of StandardValues.scala to Map(ETAG -> Seq("bar"))
      when(headers.get(testKey)) thenReturn testValue

      val target = new RememberedEtagLastModifiedHeaders(headers)
      target.get(testKey) must beEqualTo(Map("If-None-Match" -> Seq("bar"))) // TODO: Change after moving of StandardValues.scala to beEqualTo(Map(IF_NONE_MATCH -> Seq("bar")))
    }

    // TODO: Change after moving of StandardValues.scala to s"prepare $IF_MODIFIED_SINCE header if headers contain $LAST_MODIFIED value" in {
    "prepare If-Modified-Since header if headers contain Last-Modified value" in {
      val headers = mock[RememberedHeaders] //(answer({ _ => emptyReply }))
      val testValue = Map("Last-Modified" -> Seq("bar")) // TODO: Change after moving of StandardValues.scala to Map(LAST_MODIFIED -> Seq("bar"))
      when(headers.get(testKey)) thenReturn testValue

      val target = new RememberedEtagLastModifiedHeaders(headers)
      target.get(testKey) must beEqualTo(Map("If-Modified-Since" -> Seq("bar"))) // TODO: Change after moving of StandardValues.scala to beEqualTo(Map(IF_MODIFIED_SINCE -> Seq("bar")))
    }
  }
}
