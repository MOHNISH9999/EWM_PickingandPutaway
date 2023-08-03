package com.company.ewm_pickingandputaway.mdui.repackhuitemset

import android.os.Build
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.*
import com.company.ewm_pickingandputaway.R
import com.company.ewm_pickingandputaway.databinding.FragmentRepackhuitemsetCreateBinding
import com.company.ewm_pickingandputaway.mdui.BundleKeys
import com.company.ewm_pickingandputaway.mdui.InterfacedFragment
import com.company.ewm_pickingandputaway.mdui.UIConstants
import com.company.ewm_pickingandputaway.repository.OperationResult
import com.company.ewm_pickingandputaway.viewmodel.repackhuitem.RepackHuItemViewModel
import com.sap.cloud.android.odata.crvwm_physicalinventory_srv_entities.RepackHuItem
import com.sap.cloud.android.odata.crvwm_physicalinventory_srv_entities.CRVWM_PHYSICALINVENTORY_SRV_EntitiesMetadata.EntityTypes
import com.sap.cloud.mobile.fiori.formcell.SimplePropertyFormCell
import com.sap.cloud.mobile.fiori.`object`.ObjectHeader
import com.sap.cloud.mobile.odata.Property
import org.slf4j.LoggerFactory

/**
 * A fragment that is used for both update and create for users to enter values for the properties. When used for
 * update, an instance of the entity is required. In the case of create, a new instance of the entity with defaults will
 * be created. The default values may not be acceptable for the OData service.
 * This fragment is either contained in a [RepackHuItemSetListActivity] in two-pane mode (on tablets) or a
 * [RepackHuItemSetDetailActivity] on handsets.
 *
 * Arguments: Operation: [OP_CREATE | OP_UPDATE]
 *            RepackHuItem if Operation is update
 */
class RepackHuItemSetCreateFragment : InterfacedFragment<RepackHuItem, FragmentRepackhuitemsetCreateBinding>() {

    /** RepackHuItem object and it's copy: the modifications are done on the copied object. */
    private lateinit var repackHuItemEntity: RepackHuItem
    private lateinit var repackHuItemEntityCopy: RepackHuItem

    /** Indicate what operation to be performed */
    private lateinit var operation: String

    /** repackHuItemEntity ViewModel */
    private lateinit var viewModel: RepackHuItemViewModel

    /** The update menu item */
    private lateinit var updateMenuItem: MenuItem

    private val isRepackHuItemValid: Boolean
        get() {
            var isValid = true
            fragmentBinding.createUpdateRepackhuitem.let { linearLayout ->
                for (i in 0 until linearLayout.childCount) {
                    val simplePropertyFormCell = linearLayout.getChildAt(i) as SimplePropertyFormCell
                    val propertyName = simplePropertyFormCell.tag as String
                    val property = EntityTypes.repackHuItem.getProperty(propertyName)
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
                    UIConstants.OP_CREATE -> resources.getString(R.string.title_create_fragment, EntityTypes.repackHuItem.localName)
                    else -> resources.getString(R.string.title_update_fragment) + " " + EntityTypes.repackHuItem.localName

                }
            }
        }

        activity?.let {
            (it as RepackHuItemSetActivity).isNavigationDisabled = true
            viewModel = ViewModelProvider(it)[RepackHuItemViewModel::class.java]
            viewModel.createResult.observe(this) { result -> onComplete(result) }
            viewModel.updateResult.observe(this) { result -> onComplete(result) }

            repackHuItemEntity = if (operation == UIConstants.OP_CREATE) {
                createRepackHuItem()
            } else {
                viewModel.selectedEntity.value!!
            }

            val workingCopy = when{ (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) -> {
                    savedInstanceState?.getParcelable<RepackHuItem>(KEY_WORKING_COPY, RepackHuItem::class.java)
                } else -> @Suppress("DEPRECATION") savedInstanceState?.getParcelable<RepackHuItem>(KEY_WORKING_COPY)
            }

            if (workingCopy == null) {
                repackHuItemEntityCopy = repackHuItemEntity.copy()
                repackHuItemEntityCopy.entityTag = repackHuItemEntity.entityTag
                repackHuItemEntityCopy.oldEntity = repackHuItemEntity
                repackHuItemEntityCopy.editLink = repackHuItemEntity.editLink
            } else {
                repackHuItemEntityCopy = workingCopy
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        currentActivity.findViewById<ObjectHeader>(R.id.objectHeader)?.let {
            it.visibility = View.GONE
        }
        fragmentBinding.repackHuItem = repackHuItemEntityCopy
        return fragmentBinding.root
    }

    override  fun  initBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentRepackhuitemsetCreateBinding.inflate(inflater, container, false)

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
        outState.putParcelable(KEY_WORKING_COPY, repackHuItemEntityCopy)
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
        if (!isRepackHuItemValid) {
            return false
        }
        (currentActivity as RepackHuItemSetActivity).isNavigationDisabled = false
        progressBar?.visibility = View.VISIBLE
        when (operation) {
            UIConstants.OP_CREATE -> {
                viewModel.create(repackHuItemEntityCopy)
            }
            UIConstants.OP_UPDATE -> viewModel.update(repackHuItemEntityCopy)
        }
        return true
    }

    /**
     * Create a new RepackHuItem instance and initialize properties to its default values
     * Nullable property will remain null
     * For offline, keys will be unset to avoid collision should more than one is created locally
     * @return new RepackHuItem instance
     */
    private fun createRepackHuItem(): RepackHuItem {
        val entity = RepackHuItem(true)
        entity.unsetDataValue(RepackHuItem.warehouseNumber)
        entity.unsetDataValue(RepackHuItem.warehouseTask)
        entity.unsetDataValue(RepackHuItem.warehouseTaskItem)
        return entity
    }

    /** Callback function to complete processing when updateResult or createResult events fired */
    private fun onComplete(result: OperationResult<RepackHuItem>) {
        progressBar?.visibility = View.INVISIBLE
        enableUpdateMenuItem(true)
        if (result.error != null) {
            (currentActivity as RepackHuItemSetActivity).isNavigationDisabled = true
            handleError(result)
        } else {
            if (operation == UIConstants.OP_UPDATE && !currentActivity.resources.getBoolean(R.bool.two_pane)) {
                viewModel.selectedEntity.value = repackHuItemEntityCopy
            }
            if (currentActivity.resources.getBoolean(R.bool.two_pane)) {
                val listFragment = currentActivity.supportFragmentManager.findFragmentByTag(UIConstants.LIST_FRAGMENT_TAG)
                (listFragment as RepackHuItemSetListFragment).refreshListData()
            }
            (currentActivity as RepackHuItemSetActivity).onBackPressedDispatcher.onBackPressed()
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
    private fun handleError(result: OperationResult<RepackHuItem>) {
        val errorMessage = when (result.operation) {
            OperationResult.Operation.UPDATE -> getString(R.string.update_failed_detail)
            OperationResult.Operation.CREATE -> getString(R.string.create_failed_detail)
            else -> throw AssertionError()
        }
        showError(errorMessage)
    }


    companion object {
        private val KEY_WORKING_COPY = "WORKING_COPY"
        private val LOGGER = LoggerFactory.getLogger(RepackHuItemSetActivity::class.java)
    }
}
