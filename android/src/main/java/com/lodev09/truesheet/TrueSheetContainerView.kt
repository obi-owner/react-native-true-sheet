package com.lodev09.truesheet

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.view.View.MeasureSpec
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.events.EventDispatcher
import com.facebook.react.views.view.ReactViewGroup

interface TrueSheetContainerViewDelegate {
  val eventDispatcher: EventDispatcher?
  fun containerViewContentDidChangeSize(width: Int, height: Int)
  fun containerViewContentDidScroll()
  fun containerViewScrollViewDidChange()
  fun containerViewHeaderDidChangeSize(width: Int, height: Int)
  fun containerViewFooterDidChangeSize(width: Int, height: Int)
  fun containerViewScrollContentDidChangeSize(height: Int)
}

/**
 * Container view that manages the sheet's content, header, and footer views.
 * Size changes are forwarded to the delegate for sheet reconfiguration.
 */
@SuppressLint("ViewConstructor")
class TrueSheetContainerView(reactContext: ThemedReactContext) :
  ReactViewGroup(reactContext),
  TrueSheetContentViewDelegate,
  TrueSheetHeaderViewDelegate,
  TrueSheetFooterViewDelegate {

  var delegate: TrueSheetContainerViewDelegate? = null

  var contentView: TrueSheetContentView? = null
  var headerView: TrueSheetHeaderView? = null
  var footerView: TrueSheetFooterView? = null

  var contentHeight: Int = 0
  var headerHeight: Int = 0
  var footerHeight: Int = 0

  // Natural content height captured before constraints are applied.
  // Used for auto sizing when scrollable is enabled.
  private var naturalContentHeight: Int = 0
  private var hasNaturalHeight: Boolean = false

  var insetAdjustment: String = "automatic"
  var scrollViewBottomInset: Int = 0
  var scrollableEnabled: Boolean = false
  var scrollableOptions: ScrollableOptions? = null
    set(value) {
      field = value
      contentView?.scrollableOptions = value
    }

  override val eventDispatcher: EventDispatcher?
    get() = delegate?.eventDispatcher

  init {
    // Allow footer to position outside container bounds
    clipChildren = false
    clipToPadding = false
  }

  fun setupScrollable() {
    val bottomInset = if (insetAdjustment == "automatic") scrollViewBottomInset else 0
    contentView?.setupScrollable(scrollableEnabled, bottomInset)
  }

  fun setupKeyboardHandler() {
    contentView?.setupKeyboardHandler()
  }

  fun cleanupKeyboardHandler() {
    contentView?.cleanupKeyboardHandler()
  }

  override fun addView(child: View?, index: Int) {
    super.addView(child, index)

    when (child) {
      is TrueSheetContentView -> {
        child.delegate = this
        child.scrollableOptions = scrollableOptions
        contentView = child
      }

      is TrueSheetHeaderView -> {
        child.delegate = this
        headerView = child
      }

      is TrueSheetFooterView -> {
        child.delegate = this
        footerView = child
      }
    }
  }

  override fun removeViewAt(index: Int) {
    when (val view = getChildAt(index)) {
      is TrueSheetContentView -> {
        view.delegate = null
        contentView = null
        contentViewDidChangeSize(0, 0)
      }

      is TrueSheetHeaderView -> {
        view.delegate = null
        headerView = null
        headerViewDidChangeSize(0, 0)
      }

      is TrueSheetFooterView -> {
        view.delegate = null
        footerView = null
        footerViewDidChangeSize(0, 0)
      }
    }

    super.removeViewAt(index)
  }

  // ==================== Natural Height Measurement ====================

  /**
   * Gets the natural content height by looking at the ScrollView's actual content.
   * When scrollable is enabled, the ScrollView expands to fill its parent but
   * its child (the actual content) has the true height we need for auto sizing.
   */
  fun measureNaturalContentHeight(maxHeight: Int): Int {
    val content = contentView ?: return 0
    if (content.width == 0) return 0

    // When scrollable is enabled, we need to find the ScrollView and get its content height
    val scrollView = content.findScrollView()
    if (scrollView != null && scrollView.childCount > 0) {
      val scrollContent = scrollView.getChildAt(0)
      val scrollContentHeight = scrollContent.height

      Log.d(TAG, "[measureNaturalContentHeight] found ScrollView, scrollContent.height=$scrollContentHeight")

      if (scrollContentHeight > 0) {
        // The natural height is the scroll content height plus any paddingTop on the ScrollView.
        // We don't add paddingBottom since it may include safe area insets or keyboard handling
        // padding that shouldn't affect the natural content size.
        val naturalHeight = scrollContentHeight + scrollView.paddingTop
        Log.d(TAG, "[measureNaturalContentHeight] returning naturalHeight=$naturalHeight (scrollContent=$scrollContentHeight + paddingTop=${scrollView.paddingTop})")
        return naturalHeight
      }
    }

    // Fallback: measure the content view with AT_MOST
    val widthSpec = MeasureSpec.makeMeasureSpec(content.width, MeasureSpec.EXACTLY)
    val heightSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)

    Log.d(TAG, "[measureNaturalContentHeight] fallback measure, content.width=${content.width}, maxHeight=$maxHeight")
    content.measure(widthSpec, heightSpec)

    Log.d(TAG, "[measureNaturalContentHeight] fallback measuredHeight=${content.measuredHeight}")
    return content.measuredHeight
  }

  /**
   * Returns the natural content height for auto sizing.
   * If scrollable pinning is enabled and we haven't captured natural height yet,
   * measures it now. Otherwise returns the current content height.
   */
  fun getNaturalContentHeight(maxHeight: Int): Int {
    Log.d(TAG, "[getNaturalContentHeight] scrollableEnabled=$scrollableEnabled, hasNaturalHeight=$hasNaturalHeight, naturalContentHeight=$naturalContentHeight, contentHeight=$contentHeight")

    if (!scrollableEnabled) {
      Log.d(TAG, "[getNaturalContentHeight] returning contentHeight=$contentHeight (scrollable disabled)")
      return contentHeight
    }

    // If we have captured natural height and it's non-zero, use it
    if (hasNaturalHeight && naturalContentHeight > 0) {
      Log.d(TAG, "[getNaturalContentHeight] returning cached naturalContentHeight=$naturalContentHeight")
      return naturalContentHeight
    }

    // Measure natural height
    val measured = measureNaturalContentHeight(maxHeight)
    Log.d(TAG, "[getNaturalContentHeight] measured=$measured")
    if (measured > 0) {
      naturalContentHeight = measured
      hasNaturalHeight = true
      Log.d(TAG, "[getNaturalContentHeight] returning measured=$measured")
      return measured
    }

    Log.d(TAG, "[getNaturalContentHeight] fallback to contentHeight=$contentHeight")
    return contentHeight
  }

  /**
   * Resets the natural height cache. Call when content structure changes.
   */
  fun resetNaturalHeight() {
    hasNaturalHeight = false
    naturalContentHeight = 0
    Log.d(TAG, "[resetNaturalHeight] cache cleared")
  }

  // ==================== Delegate Implementations ====================

  override fun contentViewDidChangeSize(width: Int, height: Int) {
    Log.d(TAG, "[contentViewDidChangeSize] width=$width, height=$height, hasNaturalHeight=$hasNaturalHeight, scrollableEnabled=$scrollableEnabled")
    contentHeight = height

    // Capture natural height on first size change if not yet captured
    if (!hasNaturalHeight && height > 0 && !scrollableEnabled) {
      naturalContentHeight = height
      hasNaturalHeight = true
      Log.d(TAG, "[contentViewDidChangeSize] captured naturalContentHeight=$naturalContentHeight")
    }

    delegate?.containerViewContentDidChangeSize(width, height)
  }

  override fun contentViewDidScroll() {
    delegate?.containerViewContentDidScroll()
  }

  override fun contentViewScrollViewDidChange() {
    delegate?.containerViewScrollViewDidChange()
  }

  override fun headerViewDidChangeSize(width: Int, height: Int) {
    headerHeight = height
    delegate?.containerViewHeaderDidChangeSize(width, height)
  }

  override fun footerViewDidChangeSize(width: Int, height: Int) {
    footerHeight = height
    delegate?.containerViewFooterDidChangeSize(width, height)
  }

  override fun scrollContentDidChangeSize(height: Int) {
    Log.d(TAG, "[scrollContentDidChangeSize] height=$height, previous naturalContentHeight=$naturalContentHeight")
    // Update the natural height with the new scroll content height
    naturalContentHeight = height
    hasNaturalHeight = true
    delegate?.containerViewScrollContentDidChangeSize(height)
  }

  companion object {
    private const val TAG = "TrueSheetContainer"
    const val TAG_NAME = "TrueSheet"
  }
}
