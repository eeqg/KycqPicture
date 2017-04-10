package com.kycq.library.picture.picker;

import android.Manifest;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.kycq.library.picture.R;

import java.util.ArrayList;

public class KPPicturePickerActivity extends AppCompatActivity {
	/** 读取文件权限 */
	private static final int PERMISSION_STORAGE = 1;
	
	/** 拍照 */
	private static final int CAMERA = 1;
	/** 裁剪 */
	private static final int CROP = 2;
	/** 预览 */
	private static final int PREVIEW = 3;
	
	/** 选择参数信息 */
	private KPPicker kpPicker;
	
	private View kpDone;
	private RecyclerView kpRecyclerViewPicture;
	private View kpAlbumLayer;
	private RecyclerView kpRecyclerViewAlbum;
	private View kpActionbar;
	private View kpAlbum;
	private TextView kpAlbumName;
	private TextView kpPreview;
	
	private Animation layerShowAnimation;
	private Animation layerHideAnimation;
	private Animation albumShowAnimation;
	private Animation albumHideAnimation;
	
	private AlbumTask albumTask;
	
	private AlbumListAdapter albumListAdapter;
	private PictureListAdapter pictureListAdapter;
	
	private CompressTask compressTask;
	
	private boolean isPreviewDone;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle bundle = getIntent().getExtras();
		if (bundle == null) {
			finish();
			return;
		}
		this.kpPicker = bundle.getParcelable(KPPicker.PICKER);
		if (this.kpPicker == null) {
			finish();
			return;
		}
		if (savedInstanceState != null) {
			this.kpPicker = savedInstanceState.getParcelable(KPPicker.PICKER);
		}
		
		observeViews();
		observeToolbar();
		alterPickCount();
		requestAlbum();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(KPPicker.PICKER, this.kpPicker);
	}
	
	private void observeViews() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = getWindow();
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.setStatusBarColor(getResources().getColor(R.color.kpStatusBarColor));
		}
		
		setContentView(R.layout.kp_activity_picture_picker);
		
		this.kpRecyclerViewPicture = (RecyclerView) findViewById(R.id.kpRecyclerViewPicture);
		
		this.kpAlbumLayer = findViewById(R.id.kpAlbumLayer);
		this.kpAlbumLayer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleAlbumAndLayer();
			}
		});
		this.kpRecyclerViewAlbum = (RecyclerView) findViewById(R.id.kpRecyclerViewAlbum);
		
		this.kpActionbar = findViewById(R.id.kpActionbar);
		this.kpAlbum = findViewById(R.id.kpAlbum);
		this.kpAlbum.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleAlbumAndLayer();
			}
		});
		this.kpAlbumName = (TextView) findViewById(R.id.kpAlbumName);
		this.kpPreview = (TextView) findViewById(R.id.kpPreview);
		this.kpPreview.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(KPPicturePickerActivity.this, KPPicturePreviewActivity.class);
				intent.putExtra(KPPicker.PICKER, KPPicturePickerActivity.this.kpPicker);
				intent.putExtra(KPPicturePreviewActivity.PREVIEW_PICTURE_POSITION, 0);
				startActivityForResult(intent, PREVIEW);
			}
		});
		
		this.layerShowAnimation = new AlphaAnimation(0F, 1F);
		this.layerShowAnimation.setDuration(400);
		this.layerHideAnimation = new AlphaAnimation(1F, 0F);
		this.layerHideAnimation.setDuration(400);
		this.layerHideAnimation.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				kpAlbumLayer.setVisibility(View.GONE);
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
			}
		});
		
		this.albumShowAnimation = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, 0F,
				Animation.RELATIVE_TO_SELF, 0F,
				Animation.RELATIVE_TO_PARENT, 1F,
				Animation.RELATIVE_TO_PARENT, 0F);
		this.albumShowAnimation.setDuration(400);
		this.albumHideAnimation = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, 0F,
				Animation.RELATIVE_TO_SELF, 0F,
				Animation.RELATIVE_TO_PARENT, 0F,
				Animation.RELATIVE_TO_PARENT, 1F);
		this.albumHideAnimation.setDuration(400);
		this.albumHideAnimation.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				kpRecyclerViewAlbum.setVisibility(View.GONE);
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
			}
		});
	}
	
	private void toggleAlbumAndLayer() {
		if (this.kpAlbumLayer.getVisibility() == View.VISIBLE) {
			this.kpAlbumLayer.startAnimation(this.layerHideAnimation);
			this.kpRecyclerViewAlbum.startAnimation(this.albumHideAnimation);
		} else {
			this.kpAlbumLayer.startAnimation(this.layerShowAnimation);
			this.kpAlbumLayer.setVisibility(View.VISIBLE);
			this.kpRecyclerViewAlbum.startAnimation(this.albumShowAnimation);
			this.kpRecyclerViewAlbum.setVisibility(View.VISIBLE);
		}
	}
	
	private void observeToolbar() {
		assert getSupportActionBar() != null;
		getSupportActionBar().setDisplayShowHomeEnabled(false);
		getSupportActionBar().setDisplayShowTitleEnabled(false);
		getSupportActionBar().setDisplayShowCustomEnabled(true);
		getSupportActionBar().setCustomView(R.layout.kp_picture_picker_toolbar);
		
		View customView = getSupportActionBar().getCustomView();
		Toolbar toolbar = (Toolbar) customView.getParent();
		toolbar.setContentInsetsAbsolute(0, 0);
		toolbar.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, getResources().getDimensionPixelSize(R.dimen.kpActionBarSize)));
		
		View kpBack = customView.findViewById(R.id.kpBack);
		kpBack.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
		this.kpDone = customView.findViewById(R.id.kpDone);
		this.kpDone.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				requestCompress();
			}
		});
	}
	
	private void alterPickCount() {
		if (this.kpPicker.pickCount == 1) {
			this.kpPreview.setText(R.string.kp_preview);
			this.kpPreview.setVisibility(View.INVISIBLE);
			this.kpPreview.setEnabled(false);
			this.kpDone.setVisibility(View.GONE);
		} else {
			this.kpPreview.setText(
					getString(R.string.kp_format_preview,
							this.kpPicker.pictureInfoList.size(), this.kpPicker.pickCount)
			);
			this.kpPreview.setEnabled(this.kpPicker.pictureInfoList.size() > 0);
			this.kpDone.setEnabled(this.kpPicker.pictureInfoList.size() > 0);
		}
	}
	
	private void requestAlbum() {
		if (!requestStoragePermission()) {
			return;
		}
		this.albumTask = new AlbumTask(
				this.kpPicker.fullAlbumName, this.kpPicker.cacheAlbumPath,
				this.getContentResolver(),
				new AlbumTask.OnAlbumListener() {
					@Override
					public void onAlbum(AlbumInfo fullAlbumInfo,
					                    AlbumInfo cacheAlbumInfo,
					                    ArrayList<AlbumInfo> albumInfoList) {
						kpPicker.fullAlbumInfo = fullAlbumInfo;
						kpPicker.cacheAlbumInfo = cacheAlbumInfo;
						kpPicker.albumInfoList = albumInfoList;
						
						selectPictureInfoList();
						
						kpActionbar.setVisibility(View.VISIBLE);
						observeAlbumList();
					}
				});
		this.albumTask.execute();
	}
	
	private void selectPictureInfoList() {
		AlbumInfo fullAlbumInfo = this.kpPicker.fullAlbumInfo;
		ArrayList<PictureInfo> pictureInfoList = this.kpPicker.pictureInfoList;
		ArrayList<PictureInfo> resultInfoList = new ArrayList<>();
		for (PictureInfo pictureInfo : pictureInfoList) {
			int index = fullAlbumInfo.pictureInfoList.indexOf(pictureInfo);
			if (index >= 0) {
				PictureInfo selectedPictureInfo = fullAlbumInfo.pictureInfoList.get(index);
				selectedPictureInfo.selected = true;
				resultInfoList.add(selectedPictureInfo);
			} else {
				pictureInfo.selected = false;
			}
		}
		this.kpPicker.pictureInfoList = resultInfoList;
	}
	
	private void observeAlbumList() {
		this.kpRecyclerViewAlbum.setLayoutManager(new LinearLayoutManager(this));
		this.kpRecyclerViewAlbum.getItemAnimator().setChangeDuration(0);
		this.kpRecyclerViewAlbum.getItemAnimator().setAddDuration(0);
		this.kpRecyclerViewAlbum.getItemAnimator().setMoveDuration(0);
		this.kpRecyclerViewAlbum.getItemAnimator().setRemoveDuration(0);
		
		this.albumListAdapter = new AlbumListAdapter(
				this, this.kpPicker,
				new AlbumListAdapter.OnAlbumListener() {
					@Override
					public void onAlbum(AlbumInfo albumInfo) {
						if (kpAlbumLayer.getVisibility() == View.VISIBLE) {
							toggleAlbumAndLayer();
						}
						kpAlbumName.setText(albumInfo.albumName);
						kpPicker.selectedAlbumInfo = albumInfo;
						observePictureList(albumInfo);
					}
				}
		);
		this.kpRecyclerViewAlbum.setAdapter(this.albumListAdapter);
		if (this.kpPicker.selectedAlbumInfo == null) {
			this.albumListAdapter.setSelectedPosition(0);
		} else {
			this.albumListAdapter.setSelectedAlbumInfo(this.kpPicker.selectedAlbumInfo);
		}
	}
	
	private void observePictureList(AlbumInfo albumInfo) {
		this.kpRecyclerViewPicture.setLayoutManager(new GridLayoutManager(this, 3));
		this.kpRecyclerViewPicture.getItemAnimator().setChangeDuration(0);
		this.kpRecyclerViewPicture.getItemAnimator().setAddDuration(0);
		this.kpRecyclerViewPicture.getItemAnimator().setMoveDuration(0);
		this.kpRecyclerViewPicture.getItemAnimator().setRemoveDuration(0);
		
		this.pictureListAdapter = new PictureListAdapter(
				this,
				this.kpPicker.pickCount == 1, albumInfo,
				new PictureListAdapter.OnPictureListener() {
					@Override
					public void onCamera() {
						showLoading();
						
						kpPicker.pictureInfo = kpPicker.createPictureInfo();
						Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
						intent.putExtra(
								MediaStore.EXTRA_OUTPUT,
								kpPicker.getPictureContentUri(KPPicturePickerActivity.this, kpPicker.pictureInfo)
						);
						startActivityForResult(intent, CAMERA);
					}
					
					@Override
					public boolean onPick(PictureInfo pictureInfo) {
						if (pictureInfo.selected) {
							pictureInfo.selected = false;
							kpPicker.pictureInfoList.remove(pictureInfo);
						} else {
							if (kpPicker.pictureInfoList.size() == kpPicker.pickCount) {
								if (kpPicker.pickCount == 1) {
									requestCompress();
								}
								return false;
							}
							pictureInfo.selected = true;
							
							if (kpPicker.pickCount == 1) {
								if (kpPicker.pickEditable) {
									kpPicker.pictureInfo = pictureInfo;
									showLoading();
									cropPicture();
								} else {
									kpPicker.pictureInfoList.add(pictureInfo);
									requestCompress();
								}
								return false;
							} else {
								kpPicker.pictureInfoList.add(pictureInfo);
							}
						}
						alterPickCount();
						return true;
					}
					
					@Override
					public void onPreview(AlbumInfo albumInfo, int position) {
						Intent intent = new Intent(KPPicturePickerActivity.this, KPPicturePreviewActivity.class);
						intent.putExtra(KPPicker.PICKER, KPPicturePickerActivity.this.kpPicker);
						intent.putExtra(KPPicturePreviewActivity.PREVIEW_PICTURE_INFO_LIST, albumInfo.pictureInfoList);
						intent.putExtra(KPPicturePreviewActivity.PREVIEW_PICTURE_POSITION, position);
						startActivityForResult(intent, PREVIEW);
					}
				}
		);
		PictureInfo pictureInfo = this.kpPicker.pictureInfo;
		if (pictureInfo != null) {
			int index = this.kpPicker.fullAlbumInfo.pictureInfoList.indexOf(pictureInfo);
			if (index < 0) {
				if (this.kpPicker.addPictureInfo(this.kpPicker.pictureInfo)) {
					this.albumListAdapter.notifyDataSetChanged();
					this.pictureListAdapter.notifyPickPicture(this.kpPicker.pictureInfo);
				}
			} else {
				pictureInfo = this.kpPicker.fullAlbumInfo.pictureInfoList.get(index);
				this.albumListAdapter.notifyDataSetChanged();
				this.pictureListAdapter.notifyPickPicture(pictureInfo);
			}
		}
		this.kpPicker.pictureInfo = null;
		
		this.kpRecyclerViewPicture.setAdapter(this.pictureListAdapter);
		
		if (this.isPreviewDone) {
			requestCompress();
		}
	}
	
	/**
	 * 裁剪处理
	 */
	private void cropPicture() {
		PictureInfo cameraPictureInfo = this.kpPicker.pictureInfo;
		this.kpPicker.pictureInfo = this.kpPicker.createPictureInfo();
		
		try {
			Uri inputUri = this.kpPicker.getPictureContentUri(this, cameraPictureInfo);
			Uri outputUri = this.kpPicker.getPictureContentUri(this, this.kpPicker.pictureInfo);
			
			Intent intent = new Intent("com.android.camera.action.CROP");
			
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
			intent.setDataAndType(inputUri, "image/*");
			intent.putExtra("crop", "true");
			if (this.kpPicker.pickAspectX > 0 && this.kpPicker.pickAspectY > 0) {
				intent.putExtra("aspectX", this.kpPicker.pickAspectX);
				intent.putExtra("aspectY", this.kpPicker.pickAspectY);
			}
			intent.putExtra("scale", false);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				intent.setClipData(ClipData.newRawUri(MediaStore.EXTRA_OUTPUT, outputUri));
			}
			intent.putExtra("return-data", false);
			intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
			intent.putExtra("noFaceDetection", false);
			startActivityForResult(intent, CROP);
		} catch (Exception ignored) {
			ignored.printStackTrace();
			this.kpPicker.pictureInfoList.add(cameraPictureInfo);
			this.kpPicker.pictureInfo = null;
			requestCompress();
		}
	}
	
	private void requestCompress() {
		if (this.albumTask != null) {
			this.albumTask.cancel(true);
		}
		if (this.compressTask != null) {
			this.compressTask.cancel(true);
		}
		
		showLoading();
		this.compressTask = new CompressTask(
				this,
				this.kpPicker.pickMaxWidth, this.kpPicker.pickMaxHeight, this.kpPicker.pickMaxSize,
				new CompressTask.OnCompressListener() {
					@Override
					public void onCompress(ArrayList<Uri> pictureUriList) {
						hideLoading();
						
						Intent data = new Intent();
						data.putExtra(KPPicker.PICKER_LIST, pictureUriList);
						setResult(RESULT_OK, data);
						finish();
					}
				});
		this.compressTask.executeCompress(this.kpPicker.pictureInfoList);
	}
	
	void showLoading() {
	}
	
	void hideLoading() {
	}
	
	@Override
	public void onBackPressed() {
		if (this.kpAlbumLayer.getVisibility() == View.VISIBLE) {
			toggleAlbumAndLayer();
		} else {
			ActivityCompat.finishAfterTransition(this);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CAMERA) {
			if (this.pictureListAdapter == null) {
				return;
			}
			PictureInfo pictureInfo = this.kpPicker.pictureInfo;
			this.kpPicker.pictureInfo = null;
			if (resultCode == RESULT_OK) {
				// 广播添加至相册
				// Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
				// intent.setData(this.kpPicker.pictureInfo.pictureUri);
				// sendBroadcast(intent);
				
				if (this.kpPicker.addPictureInfo(pictureInfo)) {
					this.albumListAdapter.notifyDataSetChanged();
					this.pictureListAdapter.notifyPickPicture(pictureInfo);
					hideLoading();
					return;
				}
			}
			pictureInfo.delete();
			hideLoading();
			return;
		}
		if (requestCode == CROP) {
			if (this.pictureListAdapter == null) {
				return;
			}
			PictureInfo pictureInfo = this.kpPicker.pictureInfo;
			this.kpPicker.pictureInfo = null;
			if (resultCode == RESULT_OK) {
				pictureInfo.obtainPictureSize();
				if (pictureInfo.isAvailable()) {
					this.kpPicker.pictureInfoList.add(pictureInfo);
					if (this.kpPicker.addPictureInfo(pictureInfo)) {
						this.albumListAdapter.notifyDataSetChanged();
						this.pictureListAdapter.notifyPickPicture(pictureInfo);
						return;
					}
				}
			}
			pictureInfo.delete();
			hideLoading();
			return;
		}
		if (requestCode == PREVIEW) {
			if (resultCode == RESULT_OK) {
				this.isPreviewDone = data.getBooleanExtra(KPPicturePreviewActivity.PREVIEW_DONE, false);
				if (this.pictureListAdapter == null) {
					KPPicker kpPreviewPicker = data.getParcelableExtra(KPPicker.PICKER);
					this.kpPicker.pictureInfoList = kpPreviewPicker.pictureInfoList;
					return;
				}
				
				ArrayList<PictureInfo> resultInfoList = new ArrayList<>();
				
				ArrayList<PictureInfo> selectedInfoList;
				ArrayList<PictureInfo> pictureInfoList;
				ArrayList<PictureInfo> previewSelectedInfoList;
				
				selectedInfoList = this.kpPicker.pictureInfoList;
				KPPicker kpPreviewPicker = data.getParcelableExtra(KPPicker.PICKER);
				previewSelectedInfoList = kpPreviewPicker.pictureInfoList;
				if (data.getBooleanExtra(KPPicturePreviewActivity.PREVIEW_PREVIEW, false)) {
					pictureInfoList = this.kpPicker.pictureInfoList;
				} else {
					pictureInfoList = this.pictureListAdapter.getAlbumInfo().pictureInfoList;
				}
				
				for (PictureInfo pictureInfo : previewSelectedInfoList) {
					int index = pictureInfoList.indexOf(pictureInfo);
					PictureInfo selectedPictureInfo = pictureInfoList.get(index);
					selectedPictureInfo.selected = true;
					resultInfoList.add(selectedPictureInfo);
				}
				for (PictureInfo pictureInfo : selectedInfoList) {
					pictureInfo.selected = resultInfoList.contains(pictureInfo);
				}
				this.kpPicker.pictureInfoList = resultInfoList;
				alterPickCount();
				pictureListAdapter.notifyDataSetChanged();
				
				if (this.isPreviewDone) {
					requestCompress();
				}
			}
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		hideLoading();
		if (this.albumTask != null) {
			this.albumTask.cancel(true);
		}
		if (this.compressTask != null) {
			this.compressTask.cancel(true);
		}
	}
	
	/**
	 * 请求读取文件权限
	 *
	 * @return true已授权
	 */
	private boolean requestStoragePermission() {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
				|| ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				ActivityCompat.requestPermissions(
						this,
						new String[]{
								Manifest.permission.READ_EXTERNAL_STORAGE,
								Manifest.permission.WRITE_EXTERNAL_STORAGE
						},
						PERMISSION_STORAGE
				);
			} else {
				ActivityCompat.requestPermissions(
						this,
						new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						PERMISSION_STORAGE
				);
			}
			return false;
		}
		return true;
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == PERMISSION_STORAGE) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				requestAlbum();
			} else {
				finish();
			}
			return;
		}
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}
}
