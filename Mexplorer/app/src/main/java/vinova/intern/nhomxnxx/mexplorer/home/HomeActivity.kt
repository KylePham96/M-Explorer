package vinova.intern.nhomxnxx.mexplorer.home

import android.Manifest
import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.GravityCompat
import androidx.fragment.app.FragmentActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.box.androidsdk.content.BoxConfig
import com.box.androidsdk.content.auth.BoxAuthentication
import com.box.androidsdk.content.models.BoxSession
import com.google.android.gms.ads.reward.RewardedVideoAdListener
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.app_bar_home.*
import kotlinx.android.synthetic.main.content_home_layout.*
import vinova.intern.nhomxnxx.mexplorer.R
import vinova.intern.nhomxnxx.mexplorer.adapter.RvHomeAdapter
import vinova.intern.nhomxnxx.mexplorer.baseInterface.BaseActivity
import vinova.intern.nhomxnxx.mexplorer.cloud.CloudActivity
import vinova.intern.nhomxnxx.mexplorer.databaseSQLite.DatabaseHandler
import vinova.intern.nhomxnxx.mexplorer.device.DeviceActivity
import vinova.intern.nhomxnxx.mexplorer.dialogs.*
import vinova.intern.nhomxnxx.mexplorer.local.LocalActivity
import vinova.intern.nhomxnxx.mexplorer.log_in_out.LogActivity
import vinova.intern.nhomxnxx.mexplorer.model.Cloud
import vinova.intern.nhomxnxx.mexplorer.model.ListCloud
import vinova.intern.nhomxnxx.mexplorer.model.User
import vinova.intern.nhomxnxx.mexplorer.setting.SettingsActivity
import vinova.intern.nhomxnxx.mexplorer.utils.CustomDiaglogFragment
import vinova.intern.nhomxnxx.mexplorer.utils.NetworkUtils
import vinova.intern.nhomxnxx.mexplorer.utils.NetworkUtils.Companion.messageNetWork
import vinova.intern.nhomxnxx.mexplorer.utils.Support
import java.io.File
import java.lang.Exception


@Suppress("DEPRECATION", "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class HomeActivity : BaseActivity(),HomeInterface.View ,
		RenameDialog.DialogListener, GoogleApiClient.OnConnectionFailedListener,
		ConfirmDeleteDialog.ConfirmListener,
		AddCloudDialog.DialogListener, BoxAuthentication.AuthListener,
		ProfileDialog.DialogListener,
		PasswordDialog.DialogListener{
	override fun savePass(pass: ByteArray) {

	}

	override fun setSwitch(isChecked: Boolean) {
	}

	private var mPresenter :HomeInterface.Presenter= HomePresenter(this,this)
	private lateinit var adapter : RvHomeAdapter
	private var listCloud : ListCloud = ListCloud()
	val RC_SIGN_IN = 9001
	var mGoogleApiClient: GoogleApiClient? = null
	var newName : String = ""
	var providerName : String = ""
	lateinit var userToken : String
	lateinit var boxSession: BoxSession
	var firstTime = false
	var firstAuth: Boolean =false

    val CAPTURE_IMAGE_REQUEST_3 = 24
	val TAKE_PROFILE_IMG_CODE = 456
	lateinit var cloud:Cloud
	private lateinit var mRewardedVideoAdListener: RewardedVideoAdListener

	val db = DatabaseHandler(this@HomeActivity)

	override fun logoutSuccess() {
		CustomDiaglogFragment.hideLoadingDialog()
		startActivity(Intent(this,LogActivity::class.java))
		finish()
	}

	override fun forceLogOut(message: String) {
		startActivity(Intent(this,LogActivity::class.java).putExtra("force",true))
		finish()
	}

	override fun setPresenter(presenter: HomeInterface.Presenter) {
		this.mPresenter = presenter
	}

	override fun showLoading(isShow: Boolean) {
        if(isShow) CustomDiaglogFragment.showLoadingDialog(supportFragmentManager)
        else CustomDiaglogFragment.hideLoadingDialog()
	}

	override fun showError(message: String) {
		CustomDiaglogFragment.hideLoadingDialog()
		Toasty.error(this,message,Toast.LENGTH_SHORT).show()
	}

	@TargetApi(Build.VERSION_CODES.M)
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		super.onCreateDrawer()
		userToken = DatabaseHandler(this).getToken()!!
		setRecyclerView()
		setGoogleAccount()
		setBox()
		super.setAdsListener(this,mPresenter,userToken)
		if (savedInstanceState==null) {
			if (!NetworkUtils.isConnectedInternet(this)){
				showError(NetworkUtils.messageNetWork)
				return
			}
			mPresenter.getList(DatabaseHandler(this).getToken())
		}
	}



	private fun setGoogleAccount(){
		val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
				.requestScopes(Scope(Scopes.DRIVE_FULL))
				.requestServerAuthCode("389228917380-ek9t84cthihvi8u4apphlojk3knd5geu.apps.googleusercontent.com",true)
				.requestEmail()
				.build()
		mGoogleApiClient = GoogleApiClient.Builder(this@HomeActivity)
				.enableAutoManage(FragmentActivity(), this)
				.addApi(Auth.GOOGLE_SIGN_IN_API, gso)
				.build()
		mGoogleApiClient?.connect()
	}

	private fun setBox(){
		BoxConfig.CLIENT_ID = "i9jieqavbpuutnbbrqdyeo44m0imegpk"
		BoxConfig.CLIENT_SECRET = "4LjQ7N3toXIXVozyXOB21tBTcCo2KX6F"
		BoxConfig.REDIRECT_URL = "https://app.box.com"
		boxSession = BoxSession(this@HomeActivity,null)
		boxSession.setSessionAuthListener(this@HomeActivity)
	}

	@RequiresApi(Build.VERSION_CODES.M)
	override fun onNavigationItemSelected(p0: MenuItem): Boolean {
		when(p0.itemId){
			R.id.home->{

			}
			R.id.signout->{
				CustomDiaglogFragment.showLoadingDialog(supportFragmentManager)
				mPresenter.logout(this, DatabaseHandler(this).getToken())
				Auth.GoogleSignInApi.signOut(mGoogleApiClient)
			}
			R.id.bookmark->{
			}
			R.id.setting -> {
				startActivityForResult(Intent(this, SettingsActivity::class.java),1997)
			}
			R.id.device_connected -> {
				val intent = Intent(this,DeviceActivity::class.java)
				startActivity(intent)
			}
		}
		drawer_layout?.closeDrawer(GravityCompat.START)
		return true
	}

	override fun showList(list: ListCloud?) {
		rvContent.hideShimmerAdapter()
		this.listCloud = list!!
		adapter.setData(list.data)
	}

	private fun setRecyclerView(){
		adapter = RvHomeAdapter(this,app_bar_home.findViewById(R.id.bottom_sheet_detail),supportFragmentManager)
		val manager = LinearLayoutManager(this)
		rvContent.layoutManager = manager
		rvContent.adapter = adapter
        rvContent.showShimmerAdapter()

		swipeContent.setOnRefreshListener {
			if (!NetworkUtils.isConnectedInternet(this)){
				showError(NetworkUtils.messageNetWork)
			}
			else {
				mPresenter.refreshList(DatabaseHandler(this).getToken())
				swipeContent.isRefreshing = false
			}
		}

		adapter.setListener(object : RvHomeAdapter.ItemClickListener{
            override fun onItemClick(cloud: Cloud) {
                this@HomeActivity.cloud = cloud

	            val mBehavior = BottomSheetBehavior.from(bottom_sheet_detail)
	            if (mBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
					mBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
				else if (cloud.type.equals("local"))
					startActivity(Intent(this@HomeActivity,LocalActivity::class.java))
				else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !firstAuth && db.getMentAuth() =="Face") {
                        captureImage(CAPTURE_IMAGE_REQUEST_3)
                    }
					else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M  &&!firstAuth&&  db.getMentAuth() =="Pattern") {
						val file = File(Environment.getExternalStorageDirectory().path +"/Temp/.auth/"+ "code.txt")
						val encoded = Support.readFileToByteArray(file)
						val templates = Support.keyy.let { Support.decrypt(it, encoded) }
						val pass = templates.toString(Charsets.UTF_8)
						PasswordDialog.newInstance(3, pass,false).show(supportFragmentManager,"fragment")
					}
                    else {
                        val intent = Intent(this@HomeActivity, CloudActivity::class.java)
                        intent.putExtra("id", cloud.root).putExtra("token", cloud.token)
                                .putExtra("type", cloud.type).putExtra("name", cloud.name)
                        startActivity(intent)
                    }
				}
			}
		})

		fab_add.setOnClickListener {
			AddCloudDialog.newInstance().show(supportFragmentManager,"Fragement")
		}

		nav_view.menu.findItem(R.id.home).isChecked = true

		mMessageReceiver = object : BroadcastReceiver() {
			override fun onReceive(p0: Context?, p1: Intent?) {
				val message = p1?.getStringExtra("message")
				when(message){
					"Signout" -> forceLogOut("You are not sign in yet")
					else -> Toasty.success(this@HomeActivity,message!!,Toast.LENGTH_SHORT).show()
				}
			}

		}
	}

	override fun onStart() {
		super.onStart()
		LocalBroadcastManager.getInstance(this).registerReceiver((mMessageReceiver),
				object : IntentFilter("MyData"){}
		)
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            9001 -> {
                val result: GoogleSignInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
                if (result.isSuccess) {
                    val account: GoogleSignInAccount = result.signInAccount!!
                    val authCode = account.serverAuthCode
                    mPresenter.sendCode(authCode!!, newName, userToken, providerName)
                }
            }
            CAPTURE_IMAGE_REQUEST_3 -> {
                if (data != null) {
                    setSwitch(false)
                    if (!NetworkUtils.isConnectedInternet(this)) {
                        showError(NetworkUtils.messageNetWork)
                        return
                    }
                    mPresenter.authentication(this@HomeActivity, data)
                }
            }
            TAKE_PROFILE_IMG_CODE -> {
                if (data != null) {

                }
            }
            1997 -> {
                if (resultCode == 1997) {
                    if (!NetworkUtils.isConnectedInternet(this)) {
                        showError(NetworkUtils.messageNetWork)
                        return
                    }
                    CustomDiaglogFragment.showLoadingDialog(supportFragmentManager)
                    mPresenter.logout(this, DatabaseHandler(this).getToken())
                    Auth.GoogleSignInApi.signOut(mGoogleApiClient)
                }
            }
        }
	}

	override fun onRename(fromPath: String, toPath: String) {
	}

	override fun onReNameCloud(newName: String, id: String, isDic: Boolean, token: String) {
		mPresenter.renameCloud(id,newName,token,DatabaseHandler(this).getToken()!!)
	}

	override fun onConfirmDelete(path: String?) {

	}

	override fun onConfirmDeleteCloud(name: String, isDic: Boolean, id: String) {
		mPresenter.deleteCloud(id,DatabaseHandler(this@HomeActivity).getToken()!!)
	}

	override fun onOptionClick(name: String,provider:String) {

		newName = name
		providerName = provider
		if (NetworkUtils.isConnectedInternet(this))
			when(provider){
				"googledrive" -> {
					val signInIntent: Intent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
					startActivityForResult(signInIntent, RC_SIGN_IN)
					Auth.GoogleSignInApi.signOut(mGoogleApiClient)
				}
				"dropbox" ->{
					firstTime = true
					com.dropbox.core.android.Auth.startOAuth2Authentication(this@HomeActivity,getString(R.string.drbx_key))
				}
				"onedrive" -> {

				}
				"box" -> {
					boxSession.authenticate(this@HomeActivity)
				}
			}
		else
			showError(messageNetWork)
	}

	override fun onResume() {
		super.onResume()
		nav_view.menu.findItem(R.id.home).isChecked = true
		if (com.dropbox.core.android.Auth.getOAuth2Token() != null && firstTime){
			if (!NetworkUtils.isConnectedInternet(this)){
				showError(NetworkUtils.messageNetWork)
				return
			}
			firstTime = false
			val token = com.dropbox.core.android.Auth.getOAuth2Token()
			mPresenter.sendCode(token,newName,userToken,providerName)
		}
	}

	override fun onConnectionFailed(p0: ConnectionResult) {

	}

	override fun refreshList(list: ListCloud?) {
		this.listCloud = list!!
		adapter.refreshData(list.data)
	}

	override fun onBackPressed() {
		if (BottomSheetBehavior.from(bottom_sheet_detail).state == BottomSheetBehavior.STATE_EXPANDED )
			BottomSheetBehavior.from(bottom_sheet_detail).state = BottomSheetBehavior.STATE_COLLAPSED
		else
			super.onBackPressed()
	}

	override fun onSaveInstanceState(outState: Bundle?) {
		super.onSaveInstanceState(outState)
		outState?.putParcelable("list_cloud",listCloud)
	}

	override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
		super.onRestoreInstanceState(savedInstanceState)
		this.listCloud = savedInstanceState?.getParcelable("list_cloud")!!
		adapter.setData(this.listCloud.data)
	}

	override fun refresh() {
		if (!NetworkUtils.isConnectedInternet(this)){
			showError(NetworkUtils.messageNetWork)
			return
		}
		else
			mPresenter.refreshList(userToken)
	}

	//box session
	override fun onLoggedOut(info: BoxAuthentication.BoxAuthenticationInfo?, ex: Exception?) {

	}
	// get access token of box
	override fun onAuthCreated(info: BoxAuthentication.BoxAuthenticationInfo?) {
		if (!NetworkUtils.isConnectedInternet(this)){
			showError(NetworkUtils.messageNetWork)
			return
		}
		val code = boxSession.authInfo.refreshToken()
		mPresenter.sendCode(code,newName,userToken,providerName)
	}

	override fun onRefreshed(info: BoxAuthentication.BoxAuthenticationInfo?) {

	}

	override fun onAuthFailure(info: BoxAuthentication.BoxAuthenticationInfo?, ex: Exception?) {

	}

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
			2222-> {
				if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
					startActivityForResult(cameraIntent, CAPTURE_IMAGE_REQUEST_3)
				} else {
					Toasty.warning(this, "Permission Denied, Please allow to proceed !", Toast.LENGTH_LONG).show()
				}
			}
	        TAKE_PROFILE_IMG_CODE->{
		        val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
		        startActivityForResult(cameraIntent, TAKE_PROFILE_IMG_CODE)
	        }
        }
    }

	@RequiresApi(Build.VERSION_CODES.M)
	private fun captureImage(code:Int) {
		if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
			requestPermissions(arrayOf(Manifest.permission.CAMERA),2222)
		}
		else {
			val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
			startActivityForResult(cameraIntent, code)
		}
	}

	override fun turnOff() {
		db.updateMentAuth(null,userToken)
		Toasty.success(this,"Success",Toast.LENGTH_SHORT).show()

	}

    override fun isAuth(isAuth: Boolean) {
		this.firstAuth = true
        if (isAuth){
            val intent = Intent(this@HomeActivity, CloudActivity::class.java)
            intent.putExtra("id", cloud.root).putExtra("token", cloud.token)
                    .putExtra("type", cloud.type).putExtra("name", cloud.name)
            startActivity(intent)
        }
    }

	override fun onUpdate(user: User) {
		mPresenter.updateUser(user.first_name!!,user.last_name!!, Uri.parse(user.avatar_url))
	}



	override fun updateUser() {
		super.loadUser()
	}

	override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
		if (ev?.action == MotionEvent.ACTION_DOWN) {
	        if (BottomSheetBehavior.from(bottom_sheet_detail).state ==BottomSheetBehavior.STATE_EXPANDED) {

	            val outRect =  Rect()
	            bottom_sheet_detail.getGlobalVisibleRect(outRect)

	            if(!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt()))
		            BottomSheetBehavior.from(bottom_sheet_detail).state = BottomSheetBehavior.STATE_COLLAPSED
            }
	    }
		return super.dispatchTouchEvent(ev)
	}
}