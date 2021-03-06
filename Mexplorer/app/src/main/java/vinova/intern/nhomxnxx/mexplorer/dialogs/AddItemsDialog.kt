package vinova.intern.nhomxnxx.mexplorer.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import vinova.intern.nhomxnxx.mexplorer.R


class AddItemsDialog : DialogFragment() {

    private var mListener: DialogListener? = null

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val dialog = BottomSheetDialog(context!!, theme)

        val view = LayoutInflater.from(activity).inflate(R.layout.add_items_dialog, null)
        dialog.setContentView(view)
        dialog.setCancelable(true)

        view.findViewById<View>(R.id.new_folder).setOnClickListener {
            dialog.dismiss()
            mListener?.onOptionClick(R.id.new_folder, null)
        }

        view.findViewById<View>(R.id.new_file).setOnClickListener {
            dialog.dismiss()
            mListener?.onOptionClick(R.id.new_file, null)
        }

        view.findViewById<View>(R.id.new_image).setOnClickListener {
            dialog.dismiss()
            mListener?.onOptionClick(R.id.new_image, null)
        }


        // control dialog width on different devices
        dialog.setOnShowListener {
            val width = resources.getDimension(R.dimen.bottom_sheet_dialog_width).toInt()
            dialog.window?.setLayout(
                    if (width == 0) ViewGroup.LayoutParams.MATCH_PARENT else width,
                    ViewGroup.LayoutParams.MATCH_PARENT)
        }

        return dialog
    }

    interface DialogListener {
        fun onOptionClick(which: Int, path: String?)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        try {
            mListener = activity as DialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException(activity.toString() + " must implement DialogListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    companion object {

        fun newInstance(): AddItemsDialog {
            return AddItemsDialog()
        }
    }


}
