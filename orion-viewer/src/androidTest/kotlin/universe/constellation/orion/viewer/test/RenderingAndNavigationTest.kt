package universe.constellation.orion.viewer.test

import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.LayoutStrategy
import universe.constellation.orion.viewer.SimpleLayoutStrategy
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.LastPageInfo
import android.graphics.Point
import android.graphics.Bitmap
import android.content.Context
import android.test.ActivityUnitTestCase
import android.content.Intent
import junit.framework.Assert
import universe.constellation.orion.viewer.LayoutPosition
import java.util.concurrent.CountDownLatch
import universe.constellation.orion.viewer.OrionImageView
import kotlin.test.assertEquals
import java.util.Arrays

/**
 * User: mike
 * Date: 19.10.13
 * Time: 19:57
 */

class RenderingAndNavigationTest : ActivityBaseTest() {

    class MyView(val imageView: OrionImageView) : OrionImageView {

        var data: Bitmap? = null;

        override fun onNewImage(bitmap: Bitmap?, info: LayoutPosition?, latch: CountDownLatch?) {
            imageView.onNewImage(bitmap, info, latch);
            this.data = bitmap;
        }

        override fun onNewBook(title: String?, pageCount: Int) {

        }
    }

    val deviceSize = Point(300, 350); //to split page on two screen - page size is 663x886
    var view: MyView? = null;

    override fun setUp() {
        super.setUp()
        view = MyView(activity.view!!)
    }

    fun testProperPagesSkip() {
        doTestProperPages(TestUtil.SICP)
    }

    fun testProperPagesAlice() {
        doTestProperPages(TestUtil.ALICE)
    }

    fun doTestProperPages(book: String) {
        val controller = prepareEngine(book)
        val screens = 21

        val nexts = arrayListOf<IntArray>()
        for (i in 1..screens) {
            processBitmap({ controller.drawNext() }, nexts)
        }

        controller.drawNext()

        val prevs = arrayListOf<IntArray>()
        for (i in 1..screens) {
            processBitmap({ controller.drawPrev() }, prevs)
        }

        for (i in 1..screens - 2) {
            Assert.assertFalse("fallen on $i", Arrays.equals(nexts[i], nexts[i + 1]))
            Assert.assertFalse("fallen on $i", Arrays.equals(prevs[i], prevs[i + 1]))
        }

        for (i in screens - 1 downTo 1) {
            println("$i")
            Assert.assertTrue("fallen on $i", Arrays.equals(nexts[i], prevs[prevs.lastIndex - i]))
        }

    }

    fun processBitmap(drawer: Function0<Unit>, list: MutableList<IntArray>) {
        drawer()
        val bitmap = view!!.data!!
        Assert.assertNotNull(bitmap)

        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        list.add(pixels)
    }

    fun prepareEngine(book: String): Controller {
        val doc = openTestBook(book)

        var layoutStrategy: LayoutStrategy = SimpleLayoutStrategy(doc, deviceSize)
        val renderer = SingleThreadRenderer(activity, view!!, layoutStrategy, doc, Bitmap.Config.ARGB_8888)
        val controller = Controller(activity, doc, layoutStrategy, renderer)


        val lastPageInfo = LastPageInfo.loadBookParameters(activity, "123")!!
        controller.changeOrinatation(lastPageInfo.screenOrientation)
        controller.init(lastPageInfo, deviceSize)

        //getSubscriptionManager()?.sendDocOpenedNotification(controller)
        activity.view!!.setDimensionAware(controller)
        return controller
    }


    override fun getOrionTestContext(): Context {
        return instrumentation!!.context!!;
    }
}