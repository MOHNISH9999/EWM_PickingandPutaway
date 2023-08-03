package com.company.ewm_pickingandputaway.mdui.physicalinventorycountset

import android.os.Build
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.*
import com.company.ewm_pickingandputaway.R
import com.company.ewm_pickingandputaway.databinding.FragmentPhysicalinventorycountsetCreateBinding
import com.company.ewm_pickingandputaway.mdui.BundleKeys
import com.company.ewm_pickingandputaway.mdui.InterfacedFragment
import com.company.ewm_pickingandputaway.mdui.UIConstants
import com.company.ewm_pickingandputaway.repository.OperationResult
import com.company.ewm_pickingandputaway.viewmodel.physicalinventorycount.PhysicalInventoryCountViewModel
import com.sap.cloud.android.odata.crvwm_physicalinventory_srv_entities.PhysicalInventoryCount
import com.sap.cloud.android.odata.crvwm_physicalinventory_srv_entities.CRVWM_PHYSICALINVENTORY_SRV_EntitiesMetadata.EntityTypes
import com.sap.cloud.mobile.fiori.formcell.SimplePropertyFormCell
import com.sap.cloud.mobile.fiori.`object`.ObjectHeader
import com.sap.cloud.mobile.odata.Property
import org.slf4j.LoggerFactory

/**
 * A fragment that is used for both update and create for users to enter values for the properties. When used for
 * update, an instance of the entity is required. In the case of create, a new instance of the entity with defaults will
 * be created. The default values may not be acceptable for the OData service.
 * This fragment is either contained in a [PhysicalInventoryCountSetListActivity] in two-pane mode (on tablets) or a
 * [PhysicalInventoryCountSetDetailActivity] on handsets.
 *
 * Arguments: Operation: [OP_CREATE | OP_UPDATE]
 *            PhysicalInventoryCount if Operation is update
 */
class PhysicalInventoryCountSetCreateFragment : InterfacedFragment<PhysicalInventoryCount, FragmentPhysicalinventorycountsetCreateBinding>() {

    /** PhysicalInventoryCount object and it's copy: the modifications are done on the copied object. */
    private lateinit var physicalInventoryCountEntity: PhysicalInventoryCount
    private lateinit var physicalInventoryCountEntityCopy: PhysicalInventoryCount

    /** Indicate what operation to be performed */
    private lateinit var operation: String

    /** physicalInventoryCountEntity ViewModel */
    private lateinit var viewModel: PhysicalInventoryCountViewModel

    /** The update menu item */
    private lateinit var updateMenuItem: MenuItem

    private val isPhysicalInventoryCountValid: Boolean
        get() {
            var isValid = true
            fragmentBinding.createUpdatePhysicalinventorycount.let { linearLayout ->
                for (i in 0 until linearLayout.childCount) {
                    val simplePropertyFormCell = linearLayout.getChildAt(i) as SimplePropertyFormCell
                    val propertyName = simplePropertyFormCell.tag as String
                    val property = EntityTypes.physicalInventoryCount.getProperty(propertyName)
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
                    UIConstants.OP_CREATE -> resources.getString(R.string.title_create_fragment, EntityTypes.physicalInventoryCount.localName)
                    else -> resources.getString(R.string.title_update_fragment) + " " + EntityTypes.physicalInventoryCount.localName

                }
            }
        }

        activity?.let {
            (it as PhysicalInventoryCountSetActivity).isNavigationDisabled = true
            viewModel = ViewModelProvider(it)[PhysicalInventoryCountViewModel::class.java]
            viewModel.createResult.observe(this) { result -> onComplete(result) }
            viewModel.updateResult.observe(this) { result -> onComplete(result) }

            physicalInventoryCountEntity = if (operation == UIConstants.OP_CREATE) {
                createPhysicalInventoryCount()
            } else {
                viewModel.selectedEntity.value!!
            }

            val workingCopy = when{ (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) -> {
                    savedInstanceState?.getParcelable<PhysicalInventoryCount>(KEY_WORKING_COPY, PhysicalInventoryCount::class.java)
                } else -> @Suppress("DEPRECATION") savedInstanceState?.getParcelable<PhysicalInventoryCount>(KEY_WORKING_COPY)
            }

            if (workingCopy == null) {
                physicalInventoryCountEntityCopy = physicalInventoryCountEntity.copy()
                physicalInventoryCountEntityCopy.entityTag = physicalInventoryCountEntity.entityTag
                physicalInventoryCountEntityCopy.oldEntity = physicalInventoryCountEntity
                physicalInventoryCountEntityCopy.editLink = physicalInventoryCountEntity.editLink
            } else {
                physicalInventoryCountEntityCopy = workingCopy
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        currentActivity.findViewById<ObjectHeader>(R.id.objectHeader)?.let {
            it.visibility = View.GONE
        }
        fragmentBinding.physicalInventoryCount = physicalInventoryCountEntityCopy
        return fragmentBinding.root
    }

    override  fun  initBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentPhysicalinventorycountsetCreateBinding.inflate(inflater, container, false)

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
        outState.putParcelable(KEY_WORKING_COPY, physicalInventoryCountEntityCopy)
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
        if (!isPhysicalInventoryCountValid) {
            return false
        }
        (currentActivity as PhysicalInventoryCountSetActivity).isNavigationDisabled = false
        progressBar?.visibility = View.VISIBLE
        when (operation) {
            UIConstants.OP_CREATE -> {
                viewModel.create(physicalInventoryCountEntityCopy)
            }
            UIConstants.OP_UPDATE -> viewModel.update(physicalInventoryCountEntityCopy)
        }
        return true
    }

    /**
     * Create a new PhysicalInventoryCount instance and initialize properties to its default values
     * Nullable property will remain null
     * For offline, keys will be unset to avoid collision should more than one is created locally
     * @return new PhysicalInventoryCount instance
     */
    private fun createPhysicalInventoryCount(): PhysicalInventoryCount {
        val entity = PhysicalInventoryCount(true)
        entity.unsetDataValue(PhysicalInventoryCount.phyInvDocGUID)
        entity.unsetDataValue(PhysicalInventoryCount.phyInvDocument)
        entity.unsetDataValue(PhysicalInventoryCount.phyInvDocItem)
        entity.unsetDataValue(PhysicalInventoryCount.phyInvDocYear)
        return entity
    }

    /** Callback function to complete processing when updateResult or createResult events fired */
    private fun onComplete(result: OperationResult<PhysicalInventoryCount>) {
        progressBar?.visibility = View.INVISIBLE
        enableUpdateMenuItem(true)
        if (result.error != null) {
            (currentActivity as PhysicalInventoryCountSetActivity).isNavigationDisabled = true
            handleError(result)
        } else {
            if (operation == UIConstants.OP_UPDATE && !currentActivity.resources.getBoolean(R.bool.two_pane)) {
                viewModel.selectedEntity.value = physicalInventoryCountEntityCopy
            }
            if (currentActivity.resources.getBoolean(R.bool.two_pane)) {
                val listFragment = currentActivity.supportFragmentManager.findFragmentByTag(UIConstants.LIST_FRAGMENT_TAG)
                (listFragment as PhysicalInventoryCountSetListFragment).refreshListData()
            }
            (currentActivity as PhysicalInventoryCountSetActivity).onBackPressedDispatcher.onBackPressed()
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
    private fun handleError(result: OperationResult<PhysicalInventoryCount>) {
        val errorMessage = when (result.operation) {
            OperationResult.Operation.UPDATE -> getString(R.string.update_failed_detail)
            OperationResult.Operation.CREATE -> getString(R.string.create_failed_detail)
            else -> throw AssertionError()
        }
        showError(errorMessage)
    }


    companion object {
        private val KEY_WORKING_COPY = "WORKING_COPY"
        private val LOGGER = LoggerFactory.getLogger(PhysicalInventoryCountSetActivity::class.java)
    }
}
