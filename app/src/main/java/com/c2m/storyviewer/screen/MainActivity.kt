package com.c2m.storyviewer.screen

import android.animation.Animator
import android.animation.ValueAnimator
import android.os.Bundle
import android.util.SparseIntArray
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.c2m.storyviewer.R
import com.c2m.storyviewer.customview.StoryPagerAdapter
import com.c2m.storyviewer.utils.CubeOutTransformer
import com.c2m.storyviewer.utils.StoryGenerator
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(),
    PageViewOperator {

    private lateinit var pagerAdapter: StoryPagerAdapter
    private var currentPage: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setUpPager()
    }

    override fun backPageView() {
        if (viewPager.currentItem > 0) {
            try {
                fakeDrag(false)
            } catch (e: Exception) {
                //NO OP
            }
        }
    }

    override fun nextPageView() {
        if (viewPager.currentItem + 1 < viewPager.adapter?.count ?: 0) {
            try {
                fakeDrag(true)
            } catch (e: Exception) {
                //NO OP
            }
        } else {
            //there is no next story
            Toast.makeText(this, "All stories displayed.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setUpPager() {
        val storyUserList = StoryGenerator.generateStories()

        pagerAdapter = StoryPagerAdapter(
            supportFragmentManager,
            storyUserList
        )
        viewPager.adapter = pagerAdapter
        viewPager.currentItem = currentPage
        viewPager.setPageTransformer(
            true,
            CubeOutTransformer()
        )
        viewPager.addOnPageChangeListener(object : PageChangeListener() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPage = position
            }

            override fun onPageScrollCanceled() {
                currentFragment()?.resumeCurrentStory()
            }
        })
    }

    private fun currentFragment(): StoryDisplayFragment? {
        return pagerAdapter.findFragmentByPosition(viewPager, currentPage) as StoryDisplayFragment
    }

    /**
     * Change ViewPage sliding programmatically(not using reflection).
     * https://tech.dely.jp/entry/2018/12/13/110000
     * What for?
     * setCurrentItem(int, boolean) changes too fast. And it cannot set animation duration.
     */
    private var prevDragPosition = 0

    private fun fakeDrag(forward: Boolean) {
        println("IT IS a fakeDrug")
        if (prevDragPosition == 0 && viewPager.beginFakeDrag()) {
            ValueAnimator.ofInt(0, viewPager.width).apply {
                duration = 400L
                interpolator = FastOutSlowInInterpolator()
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(p0: Animator?) {}

                    override fun onAnimationEnd(animation: Animator?) {
                        removeAllUpdateListeners()
                        if (viewPager.isFakeDragging) {
                            viewPager.endFakeDrag()
                        }
                        prevDragPosition = 0
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                        removeAllUpdateListeners()
                        if (viewPager.isFakeDragging) {
                            viewPager.endFakeDrag()
                        }
                        prevDragPosition = 0
                    }

                    override fun onAnimationStart(p0: Animator?) {}
                })
                addUpdateListener {
                    if (!viewPager.isFakeDragging) return@addUpdateListener
                    val dragPosition: Int = it.animatedValue as Int
                    val dragOffset: Float =
                        ((dragPosition - prevDragPosition) * if (forward) -1 else 1).toFloat()
                    prevDragPosition = dragPosition
                    viewPager.fakeDragBy(dragOffset)
                }
            }.start()
        }
    }

    companion object {
        val progressState = SparseIntArray()
    }
}
