package com.company.ewm_pickingandputaway.mdui.physicalinventorycountset

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import com.company.ewm_pickingandputaway.databinding.FragmentPhysicalinventorycountsetDetailBinding
import com.company.ewm_pickingandputaway.mdui.EntityKeyUtil
import com.company.ewm_pickingandputaway.mdui.InterfacedFragment
import com.company.ewm_pickingandputaway.mdui.UIConstants
import com.company.ewm_pickingandputaway.repository.OperationResult
import com.company.ewm_pickingandputaway.R
import com.company.ewm_pickingandputaway.viewmodel.physicalinventorycount.PhysicalInventoryCountViewModel
import com.sap.cloud.android.odata.crvwm_physicalinventory_srv_entities.CRVWM_PHYSICALINVENTORY_SRV_EntitiesMetadata.EntitySets
import com.sap.cloud.android.odata.crvwm_physicalinventory_srv_entities.PhysicalInventoryCount
import com.sap.cloud.mobile.fiori.`object`.ObjectHeader


/**
 * A fragment representing a single PhysicalInventoryCount detail screen.
 * This fragment is contained in an PhysicalInventoryCountSetActivity.
 */
class PhysicalInventoryCountSetDetailFragment : InterfacedFragment<PhysicalInventoryCount, FragmentPhysicalinventorycountsetDetailBinding>() {

    /** PhysicalInventoryCount entity to be displayed */
    private lateinit var physicalInventoryCountEntity: PhysicalInventoryCount

    /** Fiori ObjectHeader component used when entity is to be displayed on phone */
    private var objectHeader: ObjectHeader? = null

    /** View model of the entity type that the displayed entity belongs to */
    private lateinit var viewModel: PhysicalInventoryCountViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menu = R.menu.itemlist_view_options
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        fragmentBinding.handler = this
        return fragmentBinding.root
    }

    override  fun  initBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentPhysicalinventorycountsetDetailBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let {
            currentActivity = it
            viewModel = ViewModelProvider(it)[PhysicalInventoryCountViewModel::class.java]
            viewModel.deleteResult.observe(viewLifecycleOwner) { result ->
                onDeleteComplete(result)
            }

            viewModel.selectedEntity.observe(viewLifecycleOwner) { entity ->
                physicalInventoryCountEntity = entity
                fragmentBinding.physicalInventoryCount = entity
                setupObjectHeader()
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.update_item -> {
                listener?.onFragmentStateChange(UIConstants.EVENT_EDIT_ITEM, physicalInventoryCountEntity)
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
    private fun onDeleteComplete(result: OperationResult<PhysicalInventoryCount>) {
        progressBar?.let {
            it.visibility = View.INVISIBLE
        }
        viewModel.removeAllSelected()
        result.error?.let {
            showError(getString(R.string.delete_failed_detail))
            return
        }
        listener?.onFragmentStateChange(UIConstants.EVENT_DELETION_COMPLETED, physicalInventoryCountEntity)
    }


    /**
     * Set detail image of ObjectHeader.
     * When the entity does not provides picture, set the first character of the masterProperty.
     */
    private fun setDetailImage(objectHeader: ObjectHeader, physicalInventoryCountEntity: PhysicalInventoryCount) {
        if (physicalInventoryCountEntity.getOptionalValue(PhysicalInventoryCount.phyInvDocGUID) != null && physicalInventoryCountEntity.getOptionalValue(PhysicalInventoryCount.phyInvDocGUID).toString().isNotEmpty()) {
            objectHeader.detailImageCharacter = physicalInventoryCountEntity.getOptionalValue(PhysicalInventoryCount.phyInvDocGUID).toString().substring(0, 1)
        } else {
            objectHeader.detailImageCharacter = "?"
        }
    }

    /**
     * Setup ObjectHeader with an instance of physicalInventoryCountEntity
     */
    private fun setupObjectHeader() {
        val secondToolbar = currentActivity.findViewById<Toolbar>(R.id.secondaryToolbar)
        if (secondToolbar != null) {
            secondToolbar.title = physicalInventoryCountEntity.entityType.localName
        } else {
            currentActivity.title = physicalInventoryCountEntity.entityType.localName
        }

        // Object Header is not available in tablet mode
        objectHeader = currentActivity.findViewById(R.id.objectHeader)
        val dataValue = physicalInventoryCountEntity.getOptionalValue(PhysicalInventoryCount.phyInvDocGUID)

        objectHeader?.let {
            it.apply {
                headline = dataValue?.toString()
                subheadline = EntityKeyUtil.getOptionalEntityKey(physicalInventoryCountEntity)
                body = "You can set the header body text here."
                footnote = "You can set the header footnote here."
                description = "You can add a detailed item description here."
            }
            it.setTag("#tag1", 0)
            it.setTag("#tag3", 2)
            it.setTag("#tag2", 1)

            setDetailImage(it, physicalInventoryCountEntity)
            it.visibility = View.VISIBLE
        }
    }
}
