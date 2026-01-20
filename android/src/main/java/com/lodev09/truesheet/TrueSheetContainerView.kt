package com.lodev09.truesheet

import android.annotation.SuppressLint
import android.view.View
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
   * Gets the natural content height for auto sizing with scrollable content.
   * When scrollable is enabled, the ScrollView/RecyclerView expands to fill its parent (via flex: 1),
   * but the actual content inside it has a different height.
   *
   * Formula: naturalHeight = nonScrollableHeight + scrollableContentHeight
   */
  fun measureNaturalContentHeight(): Int {
    val content = contentView ?: return 0
    if (content.width == 0 || contentHeight == 0) return 0

    val scrollableInfo = content.findScrollableViewInfo()
    if (scrollableInfo != null && scrollableInfo.contentHeight > 0 && scrollableInfo.containerHeight > 0) {
      val nonScrollableHeight = content.getNonScrollableContentHeight(scrollableInfo.view)
      return nonScrollableHeight + scrollableInfo.contentHeight
    }

    return contentHeight
  }

  /**
   * Returns the natural content height for auto sizing.
   * Called when scrollable + auto detent is used.
   */
  fun getNaturalContentHeight(): Int {
    if (hasNaturalHeight && naturalContentHeight > 0) {
      return naturalContentHeight
    }

    val measured = measureNaturalContentHeight()
    if (measured > 0) {
      naturalContentHeight = measured
      hasNaturalHeight = true
      return measured
    }

    return contentHeight
  }

  /**
   * Resets the natural height cache. Call when content structure changes.
   */
  fun resetNaturalHeight() {
    hasNaturalHeight = false
    naturalContentHeight = 0
  }

  // ==================== Delegate Implementations ====================

  override fun contentViewDidChangeSize(width: Int, height: Int) {
    contentHeight = height
    hasNaturalHeight = false
    naturalContentHeight = 0
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
    val newNaturalHeight = measureNaturalContentHeight()
    if (newNaturalHeight > 0) {
      naturalContentHeight = newNaturalHeight
      hasNaturalHeight = true
    }
    delegate?.containerViewScrollContentDidChangeSize(newNaturalHeight)
  }

  companion object {
    const val TAG_NAME = "TrueSheet"
  }
}
