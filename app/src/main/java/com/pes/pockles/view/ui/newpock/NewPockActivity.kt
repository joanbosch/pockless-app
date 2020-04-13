package com.pes.pockles.view.ui.newpock

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BasicGridItem
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.bottomsheets.gridItems
import com.afollestad.materialdialogs.bottomsheets.setPeekHeight
import com.google.android.material.snackbar.Snackbar
import com.pes.pockles.R
import com.pes.pockles.data.Resource
import com.pes.pockles.databinding.ActivityNewPockBinding
import com.pes.pockles.model.Location
import com.pes.pockles.model.Pock
import com.pes.pockles.util.LocationUtils.Companion.getLastLocation
import com.pes.pockles.view.ui.base.BaseActivity
import java.io.FileNotFoundException
import java.io.InputStream

class NewPockActivity : BaseActivity() {

    private lateinit var binding: ActivityNewPockBinding
    private val viewModel: NewPockViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(NewPockViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_new_pock)
        binding.lifecycleOwner = this
        binding.newPockViewModel = viewModel

        initializeObservers()

        binding.closeButton.setOnClickListener {
            finish()
        }

        binding.pockButton.setOnClickListener {
            getLastLocation(this, {
                viewModel.insertPock(Location(it.latitude, it.longitude))
            }, {
                handleError(true)
            })
        }

        val spinner = binding.categoriesDropdown
        spinner.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                resources.getStringArray(R.array.categories)
            )
        )

        viewModel.goUploadImage.observe(this, Observer<Boolean> { uploadImageButtonPressed ->
            if (uploadImageButtonPressed) goUploadImage()
        })
    }

    private fun handleSuccess() {
        hideLoading()
        Toast.makeText(this, resources.getString(R.string.added_pock_message), Toast.LENGTH_SHORT)
            .show()
        finish()
    }

    private fun handleError(apiError: Boolean) {
        hideLoading()
        if (apiError)
            Toast.makeText(
                this,
                resources.getString(R.string.api_error_message),
                Toast.LENGTH_SHORT
            )
                .show()
        else
            binding.pockContentField.error = resources.getString(R.string.pock_content_error)
    }

    private fun initializeObservers() {
        //It will handle the behavior of the app when we try to insert a pock into DB
        viewModel.networkCallback.observe(
            this,
            Observer { value: Resource<Pock>? ->
                value?.let {
                    when (value) {
                        is Resource.Success<*> -> handleSuccess()
                        is Resource.Error -> {
                            handleError(true)
                        }
                        is Resource.Loading -> showLoading()
                    }
                }
            })
        //In case there are any error
        viewModel.errorHandlerCallback.observe(
            this,
            Observer { value: Boolean ->
                value.let {
                    if (value)
                        handleError(false)
                }
            })
    }

    private fun showLoading() {
        val inputManager: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(
            currentFocus?.windowToken,
            InputMethodManager.SHOW_FORCED
        )

        binding.newPockProgressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.newPockProgressBar.visibility = View.GONE
    }

    private fun goUploadImage() {
        val items = listOf(
            BasicGridItem(R.drawable.ic_icon_camera, getString(R.string.take_photo_dialog_option)),
            BasicGridItem(R.drawable.ic_image, getString(R.string.select_photo_dialog_option))
        )

        MaterialDialog(this, BottomSheet()).show {
            title(text = getString(R.string.upload_image_dialog_title))
            cornerRadius(16f)
            setPeekHeight(res = R.dimen.register_menu_peek_height)
            gridItems(items) { _, index, _ ->
                run {
                    if (index == 0) {
                        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                            takePictureIntent.resolveActivity(packageManager)?.also {
                                startActivityForResult(takePictureIntent, 112)
                            }
                        }
                    } else {
                        val photoPickerIntent = Intent(Intent.ACTION_PICK)
                        photoPickerIntent.type = "image/*"
                        startActivityForResult(photoPickerIntent, 111)
                    }
                }
            }
        }
    }

    private fun setPhoto(bitmap: Bitmap) {
        viewModel.uploadMedia(bitmap).observe(this, Observer {
            when (it) {
                is Resource.Loading -> {
                    binding.loading.visibility = View.VISIBLE
                    binding.insertImageButton.visibility = View.GONE
                }
                is Resource.Error -> {
                    binding.loading.visibility = View.GONE
                    binding.insertImageButton.visibility = View.VISIBLE
                    Snackbar.make(
                        binding.newPock,
                        getString(R.string.error_uploading_an_image),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                is Resource.Success<String> -> {
                    //binding.insertImageButton.setImageBitmap(bitmap)
                    binding.insertImageButton.visibility = View.GONE
                    binding.loading.visibility = View.GONE
                    viewModel.setImageUrl(it.data)
                }
            }
        })
    }

    override fun onActivityResult(reqCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(reqCode, resultCode, data)
        if (reqCode == 111) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    val imageUri: Uri? = data?.data
                    val imageStream: InputStream? = contentResolver.openInputStream(imageUri!!)
                    val selectedImage = BitmapFactory.decodeStream(imageStream)
                    setPhoto(selectedImage)
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                    Snackbar.make(
                        binding.newPock,
                        getString(R.string.error_selecting_image),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        } else if (reqCode == 112) {
            if (resultCode == Activity.RESULT_OK) {
                val imageBitmap = data?.extras?.get("data") as Bitmap
                setPhoto(imageBitmap)
            }
        }
    }

}
