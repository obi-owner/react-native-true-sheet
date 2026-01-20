package com.lodev09.truesheet

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.facebook.react.uimanager.PixelUtil.dpToPx
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.views.view.ReactViewGroup
import com.lodev09.truesheet.core.TrueSheetKeyboardObserver
import com.lodev09.truesheet.core.TrueSheetKeyboardObserverDelegate
import com.lodev09.truesheet.utils.isDescendantOf
import com.lodev09.truesheet.utils.smoothScrollBy
import com.lodev09.truesheet.utils.smoothScrollTo

data class ScrollableOptions(val keyboardScrollOffset: Float = 0f, val scrollingExpandsSheet: Boolean = true)

/**
 * Wrapper for scrollable view info (either ScrollView or RecyclerView)
 */
data class ScrollableViewInfo(
  val view: View,
  val containerHeight: Int,
  val contentHeight: Int,
  val paddingTop: Int,
  val paddingBottom: Int
)

/**
 * Delegate interface for content view size changes
 */
interface TrueSheetContentViewDelegate {
  fun contentViewDidChangeSize(width: Int, height: Int)
  fun contentViewDidScroll()
  fun contentViewScrollViewDidChange()
  fun scrollContentDidChangeSize(height: Int)
}

/**
 * Content view that holds the main sheet content
 * This is the first child of TrueSheetContainerView
 */
@SuppressLint("ViewConstructor")
class TrueSheetContentView(private val reactContext: ThemedReactContext) : ReactViewGroup(reactContext) {
  var delegate: TrueSheetContentViewDelegate? = null

  private var lastWidth = 0
  private var lastHeight = 0

  private var pinnedScrollView: ViewGroup? = null
  private var originalScrollViewPaddingBottom: Int = 0
  private var bottomInset: Int = 0
  private var scrollExpansionPadding: Int = 0
  private var lastScrollContentHeight: Int = 0
  private var lastScrollChildrenHeight: Int = 0
  private var scrollContentLayoutListener: View.OnLayoutChangeListener? = null

  private var keyboardScrollOffset: Float = 0f
  private var keyboardObserver: TrueSheetKeyboardObserver? = null

  var scrollableOptions: ScrollableOptions? = null
    set(value) {
      field = value
      keyboardScrollOffset = value?.keyboardScrollOffset?.dpToPx() ?: 0f
    }

  override fun addView(child: View?, index: Int) {
    super.addView(child, index)
    checkScrollViewChanged()
  }

  override fun removeViewAt(index: Int) {
    super.removeViewAt(index)
    checkScrollViewChanged()
  }

  private fun checkScrollViewChanged() {
    if (pinnedScrollView == null || pinnedScrollView?.isDescendantOf(this) == false) {
      delegate?.contentViewScrollViewDidChange()
    }
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)

    if (w != lastWidth || h != lastHeight) {
      lastWidth = w
      lastHeight = h
      delegate?.contentViewDidChangeSize(w, h)
    }
  }

  fun setupScrollable(enabled: Boolean, bottomInset: Int) {
    if (!enabled) {
      clearScrollable()
      return
    }

    // Check if pinned scroll view is still valid (still in view hierarchy)
    if (pinnedScrollView != null && pinnedScrollView?.isDescendantOf(this) == false) {
      clearScrollable()
    }

    // Already set up with same inset and valid scroll view
    if (pinnedScrollView != null && this.bottomInset == bottomInset) {
      return
    }

    val scrollView = findScrollView(this) ?: return

    // Only capture originals on first pin
    if (pinnedScrollView == null) {
      originalScrollViewPaddingBottom = scrollView.paddingBottom
      pinnedScrollView = scrollView

      scrollView.isNestedScrollingEnabled = true
      (scrollView.parent as? SwipeRefreshLayout)?.isNestedScrollingEnabled = false

      scrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
        if (scrollY != oldScrollY) {
          delegate?.contentViewDidScroll()
        }
      }

      setupScrollContentListener(scrollView)
    }

    this.bottomInset = bottomInset

    setScrollViewPaddingBottom(originalScrollViewPaddingBottom + bottomInset)

    // If keyboard is currently showing, re-apply the keyboard inset to the new ScrollView
    val keyboardHeight = keyboardObserver?.currentHeight ?: 0
    if (keyboardHeight > 0) {
      setScrollViewPaddingBottom(originalScrollViewPaddingBottom + keyboardHeight)
    }
  }

  // TODO: Replace this workaround with synchronous state layout updates on every sheet resize.
  // The container is currently sized to the largest detent, so at smaller detents the ScrollView
  // viewport extends beyond the visible area, reducing the effective scroll range. This padding
  // compensates for that difference until we can resize the container per-detent synchronously.
  fun updateScrollExpansionPadding(padding: Int) {
    if (scrollExpansionPadding == padding) return
    scrollExpansionPadding = padding
    val keyboardHeight = keyboardObserver?.currentHeight ?: 0
    val basePadding = if (keyboardHeight > 0) keyboardHeight else bottomInset
    setScrollViewPaddingBottom(originalScrollViewPaddingBottom + basePadding)
    nudgeScrollView()
  }

  private fun setScrollViewPaddingBottom(paddingBottom: Int) {
    val scrollView = pinnedScrollView ?: return
    scrollView.clipToPadding = false
    scrollView.setPadding(
      scrollView.paddingLeft,
      scrollView.paddingTop,
      scrollView.paddingRight,
      paddingBottom + scrollExpansionPadding
    )
  }

  private fun setupScrollContentListener(scrollView: ViewGroup?) {
    val scrollContent = scrollView?.getChildAt(0) as? ViewGroup ?: return

    lastScrollContentHeight = scrollContent.height
    lastScrollChildrenHeight = getMaxChildBottom(scrollContent)

    scrollContentLayoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
      val newHeight = scrollContent.height
      val newChildrenHeight = getMaxChildBottom(scrollContent)

      val heightChanged = newHeight != lastScrollContentHeight && newHeight > 0
      val childrenChanged = newChildrenHeight != lastScrollChildrenHeight && newChildrenHeight > 0

      if (heightChanged || childrenChanged) {
        lastScrollContentHeight = newHeight
        lastScrollChildrenHeight = newChildrenHeight
        delegate?.scrollContentDidChangeSize(newHeight)
      }
    }
    scrollContent.addOnLayoutChangeListener(scrollContentLayoutListener)
  }

  private fun getMaxChildBottom(container: ViewGroup): Int {
    var maxBottom = 0
    for (i in 0 until container.childCount) {
      maxBottom = maxOf(maxBottom, container.getChildAt(i).bottom)
    }
    return maxBottom
  }

  private fun removeScrollContentListener() {
    val scrollContent = pinnedScrollView?.getChildAt(0)
    scrollContentLayoutListener?.let { listener ->
      scrollContent?.removeOnLayoutChangeListener(listener)
    }
    scrollContentLayoutListener = null
    lastScrollContentHeight = 0
    lastScrollChildrenHeight = 0
  }

  fun clearScrollable() {
    removeScrollContentListener()
    pinnedScrollView?.setOnScrollChangeListener(null as View.OnScrollChangeListener?)
    pinnedScrollView?.isNestedScrollingEnabled = false
    (pinnedScrollView?.parent as? SwipeRefreshLayout)?.isNestedScrollingEnabled = true
    scrollExpansionPadding = 0
    setScrollViewPaddingBottom(originalScrollViewPaddingBottom)
    pinnedScrollView = null
    originalScrollViewPaddingBottom = 0
    bottomInset = 0
  }

  fun findScrollView(): ViewGroup? {
    if (pinnedScrollView != null) return pinnedScrollView
    return findScrollView(this as View)
  }

  private fun findScrollView(view: View): ViewGroup? {
    if (view is ScrollView || view is NestedScrollView) {
      return view as ViewGroup
    }

    if (view is ViewGroup) {
      for (i in 0 until view.childCount) {
        val scrollView = findScrollView(view.getChildAt(i))
        if (scrollView != null) {
          return scrollView
        }
      }
    }

    return null
  }

  /**
   * Finds any scrollable view (ScrollView or RecyclerView) and returns its info.
   * RecyclerView is used by FlatList/FlashList in React Native.
   */
  fun findScrollableViewInfo(): ScrollableViewInfo? = findScrollableView(this)

  /**
   * Get the height of non-scrollable content above the scrollable view.
   * Calculates the Y offset by walking up the view hierarchy.
   */
  fun getNonScrollableContentHeight(scrollableView: View): Int {
    var offset = 0
    var currentView: View? = scrollableView
    while (currentView != null && currentView != this) {
      offset += currentView.top
      currentView = currentView.parent as? View
    }
    return offset
  }

  private fun findScrollableView(view: View): ScrollableViewInfo? {
    when (view) {
      is ScrollView -> {
        val contentContainer = view.getChildAt(0) as? ViewGroup
        var contentHeight = contentContainer?.height ?: 0

        // For virtualized lists (FlatList), content fills container exactly
        // Use max child bottom position to get actual content height
        if (contentContainer != null && contentHeight == view.height && contentContainer.childCount > 0) {
          val maxBottom = getMaxChildBottom(contentContainer)
          if (maxBottom > 0) {
            contentHeight = maxOf(contentHeight, maxBottom)
          }
        }

        return ScrollableViewInfo(
          view = view,
          containerHeight = view.height,
          contentHeight = contentHeight,
          paddingTop = view.paddingTop,
          paddingBottom = view.paddingBottom
        )
      }
      is RecyclerView -> {
        val scrollRange = view.computeVerticalScrollRange()
        val layoutManager = view.layoutManager

        // For small lists that fit on screen, sum visible children heights
        val actualContentHeight = if (scrollRange <= view.height && layoutManager != null) {
          var totalHeight = 0
          for (i in 0 until view.childCount) {
            totalHeight += view.getChildAt(i).height
          }
          totalHeight
        } else {
          scrollRange - view.paddingTop - view.paddingBottom
        }

        return ScrollableViewInfo(
          view = view,
          containerHeight = view.height,
          contentHeight = actualContentHeight,
          paddingTop = view.paddingTop,
          paddingBottom = view.paddingBottom
        )
      }
      is ViewGroup -> {
        for (i in 0 until view.childCount) {
          val info = findScrollableView(view.getChildAt(i))
          if (info != null) return info
        }
      }
    }
    return null
  }

  // ==================== Keyboard Handling ====================

  fun setupKeyboardHandler() {
    if (keyboardObserver != null) return

    keyboardObserver = TrueSheetKeyboardObserver(this, reactContext).apply {
      delegate = object : TrueSheetKeyboardObserverDelegate {
        override fun keyboardWillShow(height: Int) {
          updateScrollViewInsetForKeyboard(height)
        }

        override fun keyboardDidShow(height: Int) {
          scrollToFocusedInput()
        }

        override fun keyboardWillHide() {
          updateScrollViewInsetForKeyboard(0)
        }

        override fun focusDidChange(newFocus: View) {
          scrollToFocusedInput()
        }
      }
      start()
    }
  }

  fun cleanupKeyboardHandler() {
    keyboardObserver?.stop()
    keyboardObserver = null
  }

  private fun updateScrollViewInsetForKeyboard(keyboardHeight: Int) {
    val scrollView = pinnedScrollView ?: return

    val totalBottomInset = if (keyboardHeight > 0) keyboardHeight else bottomInset
    setScrollViewPaddingBottom(originalScrollViewPaddingBottom + totalBottomInset)

    scrollView.post { nudgeScrollView() }
  }

  private fun nudgeScrollView() {
    val scrollView = pinnedScrollView ?: return
    scrollView.smoothScrollBy(0, 1)
    scrollView.smoothScrollBy(0, -1)
  }

  private fun scrollToFocusedInput() {
    val scrollView = pinnedScrollView ?: findScrollView() ?: return
    val focusedView = findFocus() ?: return

    val focusedLocation = IntArray(2)
    val scrollViewLocation = IntArray(2)
    focusedView.getLocationOnScreen(focusedLocation)
    scrollView.getLocationOnScreen(scrollViewLocation)

    val relativeTop = focusedLocation[1] - scrollViewLocation[1] + scrollView.scrollY
    val relativeBottom = relativeTop + focusedView.height + keyboardScrollOffset.toInt()

    val visibleHeight = scrollView.height - scrollView.paddingBottom
    val visibleBottom = scrollView.scrollY + visibleHeight

    if (relativeBottom > visibleBottom) {
      scrollView.smoothScrollTo(0, relativeBottom - visibleHeight)
    }
  }

  companion object {
    const val TAG_NAME = "TrueSheet"
  }
}
