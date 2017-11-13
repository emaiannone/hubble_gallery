package com.derek_s.hubble_gallery.detailspage;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.derek_s.hubble_gallery.R;
import com.derek_s.hubble_gallery._shared.model.DetailsObject;
import com.derek_s.hubble_gallery.base.ActBase;
import com.derek_s.hubble_gallery.imageviewer.ImageViewerActivity;
import com.derek_s.hubble_gallery.internal.di.ActivityComponent;
import com.derek_s.hubble_gallery.utils.Animation.SquareFlipper;
import com.derek_s.hubble_gallery.utils.FavoriteUtils;
import com.derek_s.hubble_gallery.utils.ui.FontFactory;
import com.derek_s.hubble_gallery.utils.ui.Toasty;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.github.ksoichiro.android.observablescrollview.ScrollUtils;
import com.nineoldandroids.view.ViewHelper;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DetailsActivity extends ActBase implements ObservableScrollViewCallbacks, DetailsContract {

  private final static int REQUEST_WRITE_FOR_SET = 1;
  private final static int REQUEST_WRITE_FOR_SAVE = 2;
  private final static int REQUEST_WRITE_FOR_SHARE = 3;

  @BindView(R.id.iv_display)
  ImageView ivDisplay;
  @BindView(R.id.tv_title)
  TextView tvTitle;
  @BindView(R.id.tv_body)
  TextView tvBody;
  @BindView(R.id.toolbar)
  Toolbar toolbar;
  @BindView(R.id.scroll)
  ObservableScrollView scrollView;
  @BindView(R.id.square)
  View square;
  @BindView(R.id.zero_state)
  View zeroState;
  @BindView(R.id.tv_zero_state_info)
  TextView tvZeroStateInfo;
  @BindView(R.id.tv_retry)
  TextView tvRetry;
  @BindView(R.id.fl_stretchy)
  FrameLayout flStretchy;
  @Inject
  DetailsPresenter presenter;
  @Inject
  FavoriteUtils favoriteUtils;
  private SquareFlipper squareFlipper = new SquareFlipper();
  private MenuItem actionFavorite;
  private Menu menu;

  @Override
  protected void onCreate(Bundle savedState) {
    super.onCreate(savedState);
    setContentView(R.layout.act_details);
    ButterKnife.bind(this);

    presenter.setView(this);
    presenter.handleIntent(getIntent());
    presenter.onCreate(savedState);

    beautifyViews();

    presenter.toolbarBgColorAlpha = ScrollUtils.getColorWithAlpha(0, presenter.darkVibrantColor);
    toolbar.setBackgroundColor(presenter.toolbarBgColorAlpha);
    toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
    toolbar.setNavigationOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onBackPressed();
        overridePendingTransition(R.anim.fade_in_shadow, R.anim.slide_out_right);
      }
    });
    toolbar.inflateMenu(R.menu.menu_act_details);
    toolbar.setOnMenuItemClickListener(presenter.getMenuItemListener());
    menu = toolbar.getMenu();
    actionFavorite = menu.findItem(R.id.action_favorite);

    tvTitle.setText(presenter.tileObject.getTitle());

    flStretchy.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        openImageViewer();
      }
    });
    scrollView.setScrollViewCallbacks(this);
  }

  @Override
  public void onStart() {
    super.onStart();
    presenter.onStart();
  }

  @Override
  public void onStop() {
    super.onStop();
    presenter.onStop();
  }

  @Override
  public void openActivityIntent(Intent intent) {
    startActivity(intent);
  }

  @Override
  public void populateDetails(DetailsObject detailsObject) {
    tvBody.setText(Html.fromHtml(detailsObject.getDescription()));
  }

  @Override
  public void openImageViewer() {
    if (presenter.successfulSrc == null) {
      Toasty.show(this, R.string.error_loading_image, Toasty.LENGTH_MEDIUM);
    } else {
      Intent intent = new Intent(DetailsActivity.this, ImageViewerActivity.class);
      intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_SRC, presenter.successfulSrc);
      startActivity(intent);
    }
  }

  @Override
  public void showLoadingAnimation(boolean show, int type) {
    /**
     * type:
     * 0 = image
     * 1 = description
     */
    switch (type) {
      case 0:
        if (show) {
          showSquareFlipper(true);
          showZeroState(false);
        } else {
          showSquareFlipper(false);

          ivDisplay.setVisibility(View.VISIBLE);
          YoYo.with(Techniques.FadeIn).duration(1000).playOn(ivDisplay);

          tvTitle.setVisibility(View.VISIBLE);
          YoYo.with(Techniques.FadeIn).duration(700).playOn(tvTitle);

          toolbar.setVisibility(View.VISIBLE);
          YoYo.with(Techniques.SlideInDown).duration(700).playOn(toolbar);

          if (presenter.detailsObject != null)
            showLoadingAnimation(false, 1);
        }
        break;
      case 1:
        if (ivDisplay != null && ivDisplay.getVisibility() == View.VISIBLE)
          if (show)
            tvBody.setVisibility(View.INVISIBLE);
          else
            tvBody.setVisibility(View.VISIBLE);
        break;
    }
  }

  private void showSquareFlipper(boolean show) {
    if (show)
      squareFlipper.startAnimation(square);
    else
      squareFlipper.stopAnimation();
  }

  @Override
  public void showZeroState(boolean show) {
    if (show) {
      zeroState.setVisibility(View.VISIBLE);
      tvBody.setVisibility(View.GONE);

      tvZeroStateInfo.setText("Unable to load description");
      tvRetry.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          presenter.loadPage();
        }
      });
    } else {
      zeroState.setVisibility(View.INVISIBLE);
    }
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
    this.overridePendingTransition(R.anim.fade_in_shadow, R.anim.slide_out_right);
  }

  @Override
  public void updateMenu() {
    if (favoriteUtils.isFavorited(presenter.tileObject)) {
      actionFavorite.setIcon(R.drawable.ic_favorite_white_24dp);
      actionFavorite.setTitle(R.string.remove_favorite);
    } else {
      actionFavorite.setIcon(R.drawable.ic_favorite_outline_white_24dp);
      actionFavorite.setTitle(R.string.add_favorite);
    }
  }

  @Override
  public void showMessage(String message) {
    Toasty.show(this, message, Toasty.LENGTH_LONG);
  }

  @Override
  public void startShare() {
    boolean permissionGranted = checkPermissions(REQUEST_WRITE_FOR_SHARE);
    if (permissionGranted) {
      presenter.performShare();
    }
  }

  @Override
  public void startSet() {
    boolean permissionGranted = checkPermissions(REQUEST_WRITE_FOR_SET);
    if (permissionGranted) {
      presenter.performSet();
    }
  }

  @Override
  public void startSave() {
    boolean permissionGranted = checkPermissions(REQUEST_WRITE_FOR_SAVE);
    if (permissionGranted) {
      presenter.performSave();
    }
  }

  private boolean checkPermissions(int requestCode) {
    int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    if (result == PackageManager.PERMISSION_GRANTED) {
      return true;
    } else {
      requestPermission(requestCode);
      return false;
    }
  }

  private void requestPermission(int requestCode) {
    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
      Toasty.show(this,
          "Write External Storage permission allows us to do store images. Please allow this permission in App Settings.", Toasty.LENGTH_LONG);
    } else {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
    }
  }


  @Override
  public ImageView getIvDisplay() {
    return ivDisplay;
  }

  @Override
  public ObservableScrollView getScrollView() {
    return scrollView;
  }

  @Override
  public void generatePalette() {
    if (ivDisplay.getDrawable() == null)
      return;
    Palette.from(((BitmapDrawable) ivDisplay.getDrawable()).getBitmap()).generate(new Palette.PaletteAsyncListener() {
      @Override
      public void onGenerated(Palette palette) {
        presenter.setPaletteColors(palette);
        tvTitle.setBackgroundColor(presenter.darkVibrantColor);
        tvBody.setLinkTextColor(presenter.lightVibrantColor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          getWindow().setStatusBarColor(presenter.darker(presenter.darkVibrantColor, 0.6f));
        }

        Palette.Swatch swatch = palette.getDarkVibrantSwatch();
        if (swatch != null) {
          tvTitle.setTextColor(swatch.getTitleTextColor());
        }
      }
    });
  }

  @Override
  public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
    presenter.scrollYPos = scrollY;
    float alpha = 1 - (float) Math.max(0, ivDisplay.getHeight() - scrollY) / ivDisplay.getHeight();
    presenter.toolbarBgColorAlpha = ScrollUtils.getColorWithAlpha(alpha, presenter.darkVibrantColor);
    toolbar.setBackgroundColor(presenter.toolbarBgColorAlpha);
    ViewHelper.setTranslationY(ivDisplay, scrollY / 2);
  }

  @Override
  public void onDownMotionEvent() {
  }

  @Override
  public void onUpOrCancelMotionEvent(ScrollState scrollState) {
  }

  private void beautifyViews() {
    tvBody.setTextIsSelectable(true);
    tvBody.setMovementMethod(LinkMovementMethod.getInstance());

    tvZeroStateInfo.setTypeface(FontFactory.getRegular(this));
    tvRetry.setTypeface(FontFactory.getCondensedLight(this));
    tvTitle.setTypeface(FontFactory.getCondensedBold(this));
    tvBody.setTypeface(FontFactory.getRegular(this));
  }

  @Override
  protected void injectComponent(ActivityComponent activityComponent) {
    activityComponent.inject(this);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      switch (requestCode) {
        case REQUEST_WRITE_FOR_SAVE:
          presenter.performSave();
          break;
        case REQUEST_WRITE_FOR_SET:
          presenter.performSet();
          break;
        case REQUEST_WRITE_FOR_SHARE:
          presenter.performShare();
          break;
      }
    } else {
      Toasty.show(this, "Cannot perform action", Toasty.LENGTH_SHORT);
    }
  }
}