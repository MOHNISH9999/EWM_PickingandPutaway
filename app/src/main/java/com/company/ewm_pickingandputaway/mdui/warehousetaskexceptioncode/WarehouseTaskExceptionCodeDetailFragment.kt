package com.company.ewm_pickingandputaway.mdui.warehousetaskexceptioncode

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import com.company.ewm_pickingandputaway.databinding.FragmentWarehousetaskexceptioncodeDetailBinding
import com.company.ewm_pickingandputaway.mdui.EntityKeyUtil
import com.company.ewm_pickingandputaway.mdui.InterfacedFragment
import com.company.ewm_pickingandputaway.mdui.UIConstants
import com.company.ewm_pickingandputaway.repository.OperationResult
import com.company.ewm_pickingandputaway.R
import com.company.ewm_pickingandputaway.viewmodel.warehousetaskexceptioncodetype.WarehouseTaskExceptionCodeTypeViewModel
import com.sap.cloud.android.odata.cds_xcrvwmxapi_whse_order_task_entities.cds_xcrvwmxapi_whse_order_task_EntitiesMetadata.EntitySets
import com.sap.cloud.android.odata.cds_xcrvwmxapi_whse_order_task_entities.WarehouseTaskExceptionCodeType
import com.sap.cloud.mobile.fiori.`object`.ObjectHeader

import com.company.ewm_pickingandputaway.mdui.warehousetask.WarehouseTaskActivity

/**
 * A fragment representing a single WarehouseTaskExceptionCodeType detail screen.
 * This fragment is contained in an WarehouseTaskExceptionCodeActivity.
 */
class WarehouseTaskExceptionCodeDetailFragment : InterfacedFragment<WarehouseTaskExceptionCodeType, FragmentWarehousetaskexceptioncodeDetailBinding>() {

    /** WarehouseTaskExceptionCodeType entity to be displayed */
    private lateinit var warehouseTaskExceptionCodeTypeEntity: WarehouseTaskExceptionCodeType

    /** Fiori ObjectHeader component used when entity is to be displayed on phone */
    private var objectHeader: ObjectHeader? = null

    /** View model of the entity type that the displayed entity belongs to */
    private lateinit var viewModel: WarehouseTaskExceptionCodeTypeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menu = R.menu.itemlist_view_options
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        fragmentBinding.handler = this
        return fragmentBinding.root
    }

    override  fun  initBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentWarehousetaskexceptioncodeDetailBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let {
            currentActivity = it
            viewModel = ViewModelProvider(it)[WarehouseTaskExceptionCodeTypeViewModel::class.java]
            viewModel.deleteResult.observe(viewLifecycleOwner) { result ->
                onDeleteComplete(result)
            }

            viewModel.selectedEntity.observe(viewLifecycleOwner) { entity ->
                warehouseTaskExceptionCodeTypeEntity = entity
                fragmentBinding.warehouseTaskExceptionCodeType = entity
                setupObjectHeader()
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.update_item -> {
                listener?.onFragmentStateChange(UIConstants.EVENT_EDIT_ITEM, warehouseTaskExceptionCodeTypeEntity)
                true
            }
            R.id.delete_item -> {
                listener?.onFragmentStateChange(UIConstants.EVENT_ASK_DELETE_CONFIRMATION,null)
                true
            }
            else -> super.onMenuItemSelected(menuItem)
        }
    }

    /**
     * Completion callback for delete operation
     *
     * @param [result] of the operation
     */
    private fun onDeleteComplete(result: OperationResult<WarehouseTaskExceptionCodeType>) {
        progressBar?.let {
            it.visibility = View.INVISIBLE
        }
        viewModel.removeAllSelected()
        result.error?.let {
            showError(getString(R.string.delete_failed_detail))
            return
        }
        listener?.onFragmentStateChange(UIConstants.EVENT_DELETION_COMPLETED, warehouseTaskExceptionCodeTypeEntity)
    }


    @Suppress("UNUSED", "UNUSED_PARAMETER") // parameter is needed because of the xml binding
    fun onNavigationClickedToWarehouseTask_to_WarehouseTask(view: View) {
        val intent = Intent(currentActivity, WarehouseTaskActivity::class.java)
        intent.putExtra("parent", warehouseTaskExceptionCodeTypeEntity)
        intent.putExtra("navigation", "to_WarehouseTask")
        startActivity(intent)
    }

    /**
     * Set detail image of ObjectHeader.
     * When the entity does not provides picture, set the first character of the masterProperty.
     */
    private fun setDetailImage(objectHeader: ObjectHeader, warehouseTaskExceptionCodeTypeEntity: WarehouseTaskExceptionCodeType) {
        if (warehouseTaskExceptionCodeTypeEntity.getOptionalValue(WarehouseTaskExceptionCodeType.warehouse) != null && warehouseTaskExceptionCodeTypeEntity.getOptionalValue(WarehouseTaskExceptionCodeType.warehouse).toString().isNotEmpty()) {
            objectHeader.detailImageCharacter = warehouseTaskExceptionCodeTypeEntity.getOptionalValue(WarehouseTaskExceptionCodeType.warehouse).toString().substring(0, 1)
        } else {
            objectHeader.detailImageCharacter = "?"
        }
    }

    /**
     * Setup ObjectHeader with an instance of warehouseTaskExceptionCodeTypeEntity
     */
    private fun setupObjectHeader() {
        val secondToolbar = currentActivity.findViewById<Toolbar>(R.id.secondaryToolbar)
        if (secondToolbar != null) {
            secondToolbar.title = warehouseTaskExceptionCodeTypeEntity.entityType.localName
        } else {
            currentActivity.title = warehouseTaskExceptionCodeTypeEntity.entityType.localName
        }

        // Object Header is not available in tablet mode
        objectHeader = currentActivity.findViewById(R.id.objectHeader)
        val dataValue = warehouseTaskExceptionCodeTypeEntity.getOptionalValue(WarehouseTaskExceptionCodeType.warehouse)

        objectHeader?.let {
            it.apply {
                headline = dataValue?.toString()
                subheadline = EntityKeyUtil.getOptionalEntityKey(warehouseTaskExceptionCodeTypeEntity)
                body = "You can set the header body text here."
                footnote = "You can set the header footnote here."
                description = "You can add a detailed item description here."
            }
            it.setTag("#tag1", 0)
            it.setTag("#tag3", 2)
            it.setTag("#tag2", 1)

            setDetailImage(it, warehouseTaskExceptionCodeTypeEntity)
            it.visibility = View.VISIBLE
        }
    }
}