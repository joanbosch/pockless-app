package com.pes.pockles.view.ui.editpock

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
import android.widget.AdapterView
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
import com.pes.pockles.databinding.ActivityEditPockBinding
import com.pes.pockles.model.EditedPock
import com.pes.pockles.model.Pock
import com.pes.pockles.view.ui.base.BaseActivity
import java.io.FileNotFoundException
import java.io.InputStream

class EditPockActivity : BaseActivity() {

    private lateinit var binding: ActivityEditPockBinding
    private val viewModel: EditPockViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(EditPockViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_pock)
        binding.lifecycleOwner = this
        binding.editPockViewModel = viewModel

        // Add back-button to toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        initializeObservers()

        binding.updatePockButton.setOnClickListener {
            viewModel.updatePock()
        }

        binding.categoriesDropdown.onItemClickListener = AdapterView.OnItemClickListener {
                parent,view,position,id->
            viewModel.setCategory(binding.categoriesDropdown.text.toString())
        }

        binding.image1button.setOnClickListener {
            viewModel.onSaveImage(1)
            goUploadImage()
        }

        binding.image2button.setOnClickListener {
            viewModel.onSaveImage(2)
            goUploadImage()
        }

        binding.image3button.setOnClickListener {
            viewModel.onSaveImage(3)
            goUploadImage()
        }

        binding.image4button.setOnClickListener {
            viewModel.onSaveImage(4)
            goUploadImage()
        }

        val spinner = binding.categoriesDropdown
        spinner.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                arrayOf(resources.getString(R.string.general_category))+resources.getStringArray(R.array.categories)
            )
        )

        val id = intent.getStringExtra("pockId")
        val editableContent = intent.extras?.get("editableContent") as EditedPock
        viewModel.fillFieldsIfEmpty(id!!, editableContent)

        spinner.setText(viewModel.getCategory(), false)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun handleSuccess() {
        hideLoading()
        Toast.makeText(this, resources.getString(R.string.updated_pock_message), Toast.LENGTH_SHORT)
            .show()
        finish()
    }

    private fun handleError(apiError: Boolean) {
        hideLoading()
        if (apiError)
            Toast.makeText(
                this,
                resources.getString(R.string.api_error_updating_message),
                Toast.LENGTH_SHORT
            )
                .show()
        else
            binding.pockContentField.error = resources.getString(R.string.pock_content_error)
    }

    private fun initializeObservers() {
        //It will handle the behavior of the app when we try to edit a pock
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

        viewModel.errorSavingImages.observe(this, Observer<Boolean> { saveButtonPressed ->
            if (saveButtonPressed) errorImages()
        })
    }

    private fun showLoading() {
        val inputManager: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(
            currentFocus?.windowToken,
            InputMethodManager.SHOW_FORCED
        )

        binding.editPockProgressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.editPockProgressBar.visibility = View.GONE
    }

    private fun errorImages() {
        Snackbar.make(
            binding.editPock,
            getString(R.string.error_uploading_images),
            Snackbar.LENGTH_LONG
        ).show()
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

    private fun setImage(bm: Bitmap) {
        //Animation control
        if (viewModel.nImg.value != 4) setVisibilityButtons()
        //Shows in the newPock the image that the user wants to upload
        when (viewModel.actImg.value) {
            1-> binding.image1.setImageBitmap(bm)
            2-> binding.image2.setImageBitmap(bm)
            3-> binding.image3.setImageBitmap(bm)
            4-> binding.image4.setImageBitmap(bm)
        }
        //Store in the viewModel the image selected by the user
        viewModel.setBm(bm)
    }

    //Function that controls the animations when the user inserts the images
    private fun setVisibilityButtons() {
        binding.image2button.visibility = View.VISIBLE
        binding.image1.visibility = View.VISIBLE
        binding.image2.visibility = View.VISIBLE
        if (viewModel.nImg.value == 2) {
            binding.image3button.visibility = View.VISIBLE
            binding.image3.visibility = View.VISIBLE
        }
        else if (viewModel.nImg.value == 3) {
            binding.image4button.visibility = View.VISIBLE
            binding.image4.visibility = View.VISIBLE
        }
    }

    override fun onActivityResult(reqCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(reqCode, resultCode, data)
        if (reqCode == 111) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    val imageUri: Uri? = data?.data
                    val imageStream: InputStream? = contentResolver.openInputStream(imageUri!!)
                    val selectedImage = BitmapFactory.decodeStream(imageStream)
                    setImage(selectedImage)
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                    Snackbar.make(
                        binding.editPock,
                        getString(R.string.error_selecting_image),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        } else if (reqCode == 112) {
            if (resultCode == Activity.RESULT_OK) {
                val imageBitmap = data?.extras?.get("data") as Bitmap
                setImage(imageBitmap)
            }
        }
    }

}