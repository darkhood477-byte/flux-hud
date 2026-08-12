import android.content.ClipData
import android.net.Uri

fun test() {
    ClipData.newUri(null, "Test", Uri.parse("content://foo"))
}
