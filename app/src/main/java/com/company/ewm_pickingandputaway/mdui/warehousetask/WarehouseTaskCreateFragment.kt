package com.company.ewm_pickingandputaway.mdui.warehousetask

import android.os.Build
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.*
import com.company.ewm_pickingandputaway.R
import com.company.ewm_pickingandputaway.databinding.FragmentWarehousetaskCreateBinding
import com.company.ewm_pickingandputaway.mdui.BundleKeys
import com.company.ewm_pickingandputaway.mdui.InterfacedFragment
import com.company.ewm_pickingandputaway.mdui.UIConstants
import com.company.ewm_pickingandputaway.repository.OperationResult
import com.company.ewm_pickingandputaway.viewmodel.warehousetasktype.WarehouseTaskTypeViewModel
import com.sap.cloud.android.odata.cds_xcrvwmxapi_whse_order_task_entities.WarehouseTaskType
import com.sap.cloud.android.odata.cds_xcrvwmxapi_whse_order_task_entities.cds_xcrvwmxapi_whse_order_task_EntitiesMetadata.EntityTypes
import com.sap.cloud.mobile.fiori.formcell.SimplePropertyFormCell
import com.sap.cloud.mobile.fiori.`object`.ObjectHeader
import com.sap.cloud.mobile.odata.Property
import org.slf4j.LoggerFactory

/**
 * A fragment that is used for both update and create for users to enter values for the properties. When used for
 * update, an instance of the entity is required. In the case of create, a new instance of the entity with defaults will
 * be created. The default values may not be acceptable for the OData service.
 * This fragment is either contained in a [WarehouseTaskListActivity] in two-pane mode (on tablets) or a
 * [WarehouseTaskDetailActivity] on handsets.
 *
 * Arguments: Operation: [OP_CREATE | OP_UPDATE]
 *            WarehouseTaskType if Operation is update
 */
class WarehouseTaskCreateFragment : InterfacedFragment<WarehouseTaskType, FragmentWarehousetaskCreateBinding>() {

    /** WarehouseTaskType object and it's copy: the modifications are done on the copied object. */
    private lateinit var warehouseTaskTypeEntity: WarehouseTaskType
    private lateinit var warehouseTaskTypeEntityCopy: WarehouseTaskType

    /** Indicate what operation to be performed */
    private lateinit var operation: String

    /** warehouseTaskTypeEntity ViewModel */
    private lateinit var viewModel: WarehouseTaskTypeViewModel

    /** The update menu item */
    private lateinit var updateMenuItem: MenuItem

    private val isWarehouseTaskTypeValid: Boolean
        get() {
            var isValid = true
            fragmentBinding.createUpdateWarehousetasktype.let { linearLayout ->
                for (i in 0 until linearLayout.childCount) {
                    val simplePropertyFormCell = linearLayout.getChildAt(i) as SimplePropertyFormCell
                    val propertyName = simplePropertyFormCell.tag as String
                    val property = EntityTypes.warehouseTaskType.getProperty(propertyName)
                    val value = simplePropertyFormCell.value.toString()
                    if (!isValidProperty(property, value)) {
                        simplePropertyFormCell.setTag(R.id.TAG_HAS_MANDATORY_ERROR, true)
                        val errorMessage = resources.getString(R.string.mandatory_warning)
                        simplePropertyFormCell.isErrorEnabled = true
                        simplePropertyFormCell.error = errorMessage
                        isValid = false
                    } else {
                        if (simplePropertyFormCell.isErrorEnabled) {
                            val hasMandatoryError = simplePropertyFormCell.getTag(R.id.TAG_HAS_MANDATORY_ERROR) as Boolean
                            if (!hasMandatoryError) {
                                isValid = false
                            } else {
                                simplePropertyFormCell.isErrorEnabled = false
                            }
                        }
                        simplePropertyFormCell.setTag(R.id.TAG_HAS_MANDATORY_ERROR, false)
                    }
                }
            }
            return isValid
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menu = R.menu.itemlist_edit_options

        arguments?.let {
            (it.getString(BundleKeys.OPERATION))?.let { operationType ->
                operation = operationType
                activityTitle = when (operationType) {
                    UIConstants.OP_CREATE -> resources.getString(R.string.title_create_fragment, EntityTypes.warehouseTaskType.localName)
                    else -> resources.getString(R.string.title_update_fragment) + " " + EntityTypes.warehouseTaskType.localName

                }
            }
        }

        activity?.let {
            (it as WarehouseTaskActivity).isNavigationDisabled = true
            viewModel = ViewModelProvider(it)[WarehouseTaskTypeViewModel::class.java]
            viewModel.createResult.observe(this) { result -> onComplete(result) }
            viewModel.updateResult.observe(this) { result -> onComplete(result) }

            warehouseTaskTypeEntity = if (operation == UIConstants.OP_CREATE) {
                createWarehouseTaskType()
            } else {
                viewModel.selectedEntity.value!!
            }

            val workingCopy = when{ (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) -> {
                savedInstanceState?.getParcelable<WarehouseTaskType>(KEY_WORKING_COPY, WarehouseTaskType::class.java)
            } else -> @Suppress("DEPRECATION") savedInstanceState?.getParcelable<WarehouseTaskType>(KEY_WORKING_COPY)
            }

            if (workingCopy == null) {
                warehouseTaskTypeEntityCopy = warehouseTaskTypeEntity.copy()
                warehouseTaskTypeEntityCopy.entityTag = warehouseTaskTypeEntity.entityTag
                warehouseTaskTypeEntityCopy.oldEntity = warehouseTaskTypeEntity
                warehouseTaskTypeEntityCopy.editLink = warehouseTaskTypeEntity.editLink
            } else {
                warehouseTaskTypeEntityCopy = workingCopy
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        currentActivity.findViewById<ObjectHeader>(R.id.objectHeader)?.let {
            it.visibility = View.GONE
        }
        fragmentBinding.warehouseTaskType = warehouseTaskTypeEntityCopy
        return fragmentBinding.root
    }

    override  fun  initBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentWarehousetaskCreateBinding.inflate(inflater, container, false)

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.save_item -> {
                updateMenuItem = menuItem
                enableUpdateMenuItem(false)
                onSaveItem()
            }
            else -> super.onMenuItemSelected(menuItem)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if(secondaryToolbar != null) secondaryToolbar!!.title = activityTitle else activity?.title = activityTitle
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(KEY_WORKING_COPY, warehouseTaskTypeEntityCopy)
        super.onSaveInstanceState(outState)
    }

    /** Enables the update menu item based on [enable] */
    private fun enableUpdateMenuItem(enable : Boolean = true) {
        updateMenuItem.also {
            it.isEnabled = enable
            it.icon?.alpha = if(enable) 255 else 130
        }
    }

    /** Saves the entity */
    private fun onSaveItem(): Boolean {
        if (!isWarehouseTaskTypeValid) {
            return false
        }
        (currentActivity as WarehouseTaskActivity).isNavigationDisabled = false
        progressBar?.visibility = View.VISIBLE
        when (operation) {
            UIConstants.OP_CREATE -> {
                viewModel.create(warehouseTaskTypeEntityCopy)
            }
            UIConstants.OP_UPDATE -> viewModel.update(warehouseTaskTypeEntityCopy)
        }
        return true
    }

    /**
     * Create a new WarehouseTaskType instance and initialize properties to its default values
     * Nullable property will remain null
     * For offline, keys will be unset to avoid collision should more than one is created locally
     * @return new WarehouseTaskType instance
     */
    private fun createWarehouseTaskType(): WarehouseTaskType {
        val entity = WarehouseTaskType(true)
        entity.unsetDataValue(WarehouseTaskType.warehouse)
        entity.unsetDataValue(WarehouseTaskType.warehouseTask)
        entity.unsetDataValue(WarehouseTaskType.warehouseTaskItem)
        return entity
    }

    /** Callback function to complete processing when updateResult or createResult events fired */
    private fun onComplete(result: OperationResult<WarehouseTaskType>) {
        progressBar?.visibility = View.INVISIBLE
        enableUpdateMenuItem(true)
        if (result.error != null) {
            (currentActivity as WarehouseTaskActivity).isNavigationDisabled = true
            handleError(result)
        } else {
            if (operation == UIConstants.OP_UPDATE && !currentActivity.resources.getBoolean(R.bool.two_pane)) {
                viewModel.selectedEntity.value = warehouseTaskTypeEntityCopy
            }
            if (currentActivity.resources.getBoolean(R.bool.two_pane)) {
                val listFragment = currentActivity.supportFragmentManager.findFragmentByTag(UIConstants.LIST_FRAGMENT_TAG)
                (listFragment as WarehouseTaskListFragment).refreshListData()
            }
            (currentActivity as WarehouseTaskActivity).onBackPressedDispatcher.onBackPressed()
        }
    }

    /** Simple validation: checks the presence of mandatory fields. */
    private fun isValidProperty(property: Property, value: String): Boolean {
        return !(!property.isNullable && value.isEmpty())
    }

    /**
     * Notify user of error encountered while execution the operation
     *
     * @param [result] operation result with error
     */
    private fun handleError(result: OperationResult<WarehouseTaskType>) {
        val errorMessage = when (result.operation) {
            OperationResult.Operation.UPDATE -> getString(R.string.update_failed_detail)
            OperationResult.Operation.CREATE -> getString(R.string.create_failed_detail)
            else -> throw AssertionError()
        }
        showError(errorMessage)
    }


    companion object {
        private val KEY_WORKING_COPY = "WORKING_COPY"
        private val LOGGER = LoggerFactory.getLogger(WarehouseTaskActivity::class.java)
    }
}