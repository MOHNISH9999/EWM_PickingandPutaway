package com.company.ewm_pickingandputaway.mdui.warehousetaskexceptioncode

import android.os.Build
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.*
import com.company.ewm_pickingandputaway.R
import com.company.ewm_pickingandputaway.databinding.FragmentWarehousetaskexceptioncodeCreateBinding
import com.company.ewm_pickingandputaway.mdui.BundleKeys
import com.company.ewm_pickingandputaway.mdui.InterfacedFragment
import com.company.ewm_pickingandputaway.mdui.UIConstants
import com.company.ewm_pickingandputaway.repository.OperationResult
import com.company.ewm_pickingandputaway.viewmodel.warehousetaskexceptioncodetype.WarehouseTaskExceptionCodeTypeViewModel
import com.sap.cloud.android.odata.cds_xcrvwmxapi_whse_order_task_entities.WarehouseTaskExceptionCodeType
import com.sap.cloud.android.odata.cds_xcrvwmxapi_whse_order_task_entities.cds_xcrvwmxapi_whse_order_task_EntitiesMetadata.EntityTypes
import com.sap.cloud.mobile.fiori.formcell.SimplePropertyFormCell
import com.sap.cloud.mobile.fiori.`object`.ObjectHeader
import com.sap.cloud.mobile.odata.Property
import org.slf4j.LoggerFactory

/**
 * A fragment that is used for both update and create for users to enter values for the properties. When used for
 * update, an instance of the entity is required. In the case of create, a new instance of the entity with defaults will
 * be created. The default values may not be acceptable for the OData service.
 * This fragment is either contained in a [WarehouseTaskExceptionCodeListActivity] in two-pane mode (on tablets) or a
 * [WarehouseTaskExceptionCodeDetailActivity] on handsets.
 *
 * Arguments: Operation: [OP_CREATE | OP_UPDATE]
 *            WarehouseTaskExceptionCodeType if Operation is update
 */
class WarehouseTaskExceptionCodeCreateFragment : InterfacedFragment<WarehouseTaskExceptionCodeType, FragmentWarehousetaskexceptioncodeCreateBinding>() {

    /** WarehouseTaskExceptionCodeType object and it's copy: the modifications are done on the copied object. */
    private lateinit var warehouseTaskExceptionCodeTypeEntity: WarehouseTaskExceptionCodeType
    private lateinit var warehouseTaskExceptionCodeTypeEntityCopy: WarehouseTaskExceptionCodeType

    /** Indicate what operation to be performed */
    private lateinit var operation: String

    /** warehouseTaskExceptionCodeTypeEntity ViewModel */
    private lateinit var viewModel: WarehouseTaskExceptionCodeTypeViewModel

    /** The update menu item */
    private lateinit var updateMenuItem: MenuItem

    private val isWarehouseTaskExceptionCodeTypeValid: Boolean
        get() {
            var isValid = true
            fragmentBinding.createUpdateWarehousetaskexceptioncodetype.let { linearLayout ->
                for (i in 0 until linearLayout.childCount) {
                    val simplePropertyFormCell = linearLayout.getChildAt(i) as SimplePropertyFormCell
                    val propertyName = simplePropertyFormCell.tag as String
                    val property = EntityTypes.warehouseTaskExceptionCodeType.getProperty(propertyName)
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
                    UIConstants.OP_CREATE -> resources.getString(R.string.title_create_fragment, EntityTypes.warehouseTaskExceptionCodeType.localName)
                    else -> resources.getString(R.string.title_update_fragment) + " " + EntityTypes.warehouseTaskExceptionCodeType.localName

                }
            }
        }

        activity?.let {
            (it as WarehouseTaskExceptionCodeActivity).isNavigationDisabled = true
            viewModel = ViewModelProvider(it)[WarehouseTaskExceptionCodeTypeViewModel::class.java]
            viewModel.createResult.observe(this) { result -> onComplete(result) }
            viewModel.updateResult.observe(this) { result -> onComplete(result) }

            warehouseTaskExceptionCodeTypeEntity = if (operation == UIConstants.OP_CREATE) {
                createWarehouseTaskExceptionCodeType()
            } else {
                viewModel.selectedEntity.value!!
            }

            val workingCopy = when{ (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) -> {
                savedInstanceState?.getParcelable<WarehouseTaskExceptionCodeType>(KEY_WORKING_COPY, WarehouseTaskExceptionCodeType::class.java)
            } else -> @Suppress("DEPRECATION") savedInstanceState?.getParcelable<WarehouseTaskExceptionCodeType>(KEY_WORKING_COPY)
            }

            if (workingCopy == null) {
                warehouseTaskExceptionCodeTypeEntityCopy = warehouseTaskExceptionCodeTypeEntity.copy()
                warehouseTaskExceptionCodeTypeEntityCopy.entityTag = warehouseTaskExceptionCodeTypeEntity.entityTag
                warehouseTaskExceptionCodeTypeEntityCopy.oldEntity = warehouseTaskExceptionCodeTypeEntity
                warehouseTaskExceptionCodeTypeEntityCopy.editLink = warehouseTaskExceptionCodeTypeEntity.editLink
            } else {
                warehouseTaskExceptionCodeTypeEntityCopy = workingCopy
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        currentActivity.findViewById<ObjectHeader>(R.id.objectHeader)?.let {
            it.visibility = View.GONE
        }
        fragmentBinding.warehouseTaskExceptionCodeType = warehouseTaskExceptionCodeTypeEntityCopy
        return fragmentBinding.root
    }

    override  fun  initBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentWarehousetaskexceptioncodeCreateBinding.inflate(inflater, container, false)

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
        outState.putParcelable(KEY_WORKING_COPY, warehouseTaskExceptionCodeTypeEntityCopy)
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
        if (!isWarehouseTaskExceptionCodeTypeValid) {
            return false
        }
        (currentActivity as WarehouseTaskExceptionCodeActivity).isNavigationDisabled = false
        progressBar?.visibility = View.VISIBLE
        when (operation) {
            UIConstants.OP_CREATE -> {
                viewModel.create(warehouseTaskExceptionCodeTypeEntityCopy)
            }
            UIConstants.OP_UPDATE -> viewModel.update(warehouseTaskExceptionCodeTypeEntityCopy)
        }
        return true
    }

    /**
     * Create a new WarehouseTaskExceptionCodeType instance and initialize properties to its default values
     * Nullable property will remain null
     * For offline, keys will be unset to avoid collision should more than one is created locally
     * @return new WarehouseTaskExceptionCodeType instance
     */
    private fun createWarehouseTaskExceptionCodeType(): WarehouseTaskExceptionCodeType {
        val entity = WarehouseTaskExceptionCodeType(true)
        entity.unsetDataValue(WarehouseTaskExceptionCodeType.warehouse)
        entity.unsetDataValue(WarehouseTaskExceptionCodeType.warehouseTask)
        entity.unsetDataValue(WarehouseTaskExceptionCodeType.warehouseTaskItem)
        entity.unsetDataValue(WarehouseTaskExceptionCodeType.warehouseTaskExceptionCode)
        entity.unsetDataValue(WarehouseTaskExceptionCodeType.businessContext)
        entity.unsetDataValue(WarehouseTaskExceptionCodeType.businessContextExecutionStep)
        return entity
    }

    /** Callback function to complete processing when updateResult or createResult events fired */
    private fun onComplete(result: OperationResult<WarehouseTaskExceptionCodeType>) {
        progressBar?.visibility = View.INVISIBLE
        enableUpdateMenuItem(true)
        if (result.error != null) {
            (currentActivity as WarehouseTaskExceptionCodeActivity).isNavigationDisabled = true
            handleError(result)
        } else {
            if (operation == UIConstants.OP_UPDATE && !currentActivity.resources.getBoolean(R.bool.two_pane)) {
                viewModel.selectedEntity.value = warehouseTaskExceptionCodeTypeEntityCopy
            }
            if (currentActivity.resources.getBoolean(R.bool.two_pane)) {
                val listFragment = currentActivity.supportFragmentManager.findFragmentByTag(UIConstants.LIST_FRAGMENT_TAG)
                (listFragment as WarehouseTaskExceptionCodeListFragment).refreshListData()
            }
            (currentActivity as WarehouseTaskExceptionCodeActivity).onBackPressedDispatcher.onBackPressed()
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
    private fun handleError(result: OperationResult<WarehouseTaskExceptionCodeType>) {
        val errorMessage = when (result.operation) {
            OperationResult.Operation.UPDATE -> getString(R.string.update_failed_detail)
            OperationResult.Operation.CREATE -> getString(R.string.create_failed_detail)
            else -> throw AssertionError()
        }
        showError(errorMessage)
    }


    companion object {
        private val KEY_WORKING_COPY = "WORKING_COPY"
        private val LOGGER = LoggerFactory.getLogger(WarehouseTaskExceptionCodeActivity::class.java)
    }
}