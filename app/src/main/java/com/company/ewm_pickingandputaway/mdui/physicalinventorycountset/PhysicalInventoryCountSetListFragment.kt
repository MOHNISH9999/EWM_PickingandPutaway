package com.company.ewm_pickingandputaway.mdui.physicalinventorycountset

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.CheckBox
import android.widget.ImageView
import com.company.ewm_pickingandputaway.databinding.ElementEntityitemListBinding
import com.company.ewm_pickingandputaway.databinding.FragmentEntityitemListBinding
import com.company.ewm_pickingandputaway.R
import com.company.ewm_pickingandputaway.viewmodel.EntityViewModelFactory
import com.company.ewm_pickingandputaway.viewmodel.physicalinventorycount.PhysicalInventoryCountViewModel
import com.company.ewm_pickingandputaway.repository.OperationResult
import com.company.ewm_pickingandputaway.mdui.UIConstants
import com.company.ewm_pickingandputaway.mdui.EntitySetListActivity.EntitySetName
import com.company.ewm_pickingandputaway.mdui.InterfacedFragment
import com.sap.cloud.android.odata.crvwm_physicalinventory_srv_entities.CRVWM_PHYSICALINVENTORY_SRV_EntitiesMetadata.EntitySets
import com.sap.cloud.android.odata.crvwm_physicalinventory_srv_entities.PhysicalInventoryCount
import com.sap.cloud.mobile.fiori.`object`.ObjectCell
import com.sap.cloud.mobile.fiori.`object`.ObjectHeader
import com.sap.cloud.mobile.odata.EntityValue
import org.slf4j.LoggerFactory

/**
 * An activity representing a list of PhysicalInventoryCount. This activity has different presentations for handset and tablet-size
 * devices. On handsets, the activity presents a list of items, which when touched, lead to a view representing
 * PhysicalInventoryCount details. On tablets, the activity presents the list of PhysicalInventoryCount and PhysicalInventoryCount details side-by-side using two
 * vertical panes.
 */

class PhysicalInventoryCountSetListFragment : InterfacedFragment<PhysicalInventoryCount, FragmentEntityitemListBinding>() {

    /**
     * List adapter to be used with RecyclerView containing all instances of physicalInventoryCountSet
     */
    private var adapter: PhysicalInventoryCountListAdapter? = null

    private lateinit var refreshLayout: SwipeRefreshLayout
    private var actionMode: ActionMode? = null
    private var isInActionMode: Boolean = false
    private val selectedItems = ArrayList<Int>()

    /**
     * View model of the entity type
     */
    private lateinit var viewModel: PhysicalInventoryCountViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityTitle = getString(EntitySetName.PhysicalInventoryCountSet.titleId)
        menu = R.menu.itemlist_menu
        savedInstanceState?.let {
            isInActionMode = it.getBoolean("ActionMode")
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        currentActivity.findViewById<ObjectHeader>(R.id.objectHeader)?.let {
            it.visibility = View.GONE
        }
        return fragmentBinding.root
    }

    override  fun  initBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentEntityitemListBinding.inflate(inflater, container, false)

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.menu_refresh -> {
                refreshLayout.isRefreshing = true
                refreshListData()
                true
            }
            else -> return super.onMenuItemSelected(menuItem)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("ActionMode", isInActionMode)
        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        currentActivity.title = activityTitle

        fragmentBinding.itemList?.let {
            this.adapter = PhysicalInventoryCountListAdapter(currentActivity, it)
            it.adapter = this.adapter
        } ?: throw AssertionError()

        setupRefreshLayout()
        refreshLayout.isRefreshing = true

        navigationPropertyName = currentActivity.intent.getStringExtra("navigation")
        parentEntityData = when {
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) -> {
                currentActivity.intent.getParcelableExtra("parent", Parcelable::class.java)
            }
            else -> @Suppress("DEPRECATION") currentActivity.intent.getParcelableExtra("parent")
        }

        fragmentBinding.fab?.let {
            it.contentDescription = getString(R.string.add_new) + " PhysicalInventoryCount"
            if (navigationPropertyName != null && parentEntityData != null) {
                it.hide()
            } else {
                it.setOnClickListener {
                    listener?.onFragmentStateChange(UIConstants.EVENT_CREATE_NEW_ITEM, null)
                }
            }
        }

        prepareViewModel()
    }

    override fun onResume() {
        super.onResume()
        refreshListData()
    }

    /** Initializes the view model and add observers on it */
    private fun prepareViewModel() {
        viewModel = if( navigationPropertyName != null && parentEntityData != null ) {
            ViewModelProvider(currentActivity, EntityViewModelFactory(currentActivity.application, navigationPropertyName!!, parentEntityData!!))
                .get(PhysicalInventoryCountViewModel::class.java)
        } else {
            ViewModelProvider(currentActivity).get(PhysicalInventoryCountViewModel::class.java)
        }
        viewModel.observableItems.observe(viewLifecycleOwner, Observer<List<PhysicalInventoryCount>> { items ->
            items?.let { entityList ->
                adapter?.let { listAdapter ->
                    listAdapter.setItems(entityList)

                    var item = viewModel.selectedEntity.value?.let { containsItem(entityList, it) }
                    if (item == null) {
                        item = if (entityList.isEmpty()) null else entityList[0]
                    }

                    item?.let {
                        viewModel.inFocusId = listAdapter.getItemIdForPhysicalInventoryCount(it)
                        if (currentActivity.resources.getBoolean(R.bool.two_pane)) {
                            viewModel.setSelectedEntity(it)
                            if(!isInActionMode && !(currentActivity as PhysicalInventoryCountSetActivity).isNavigationDisabled) {
                                listener?.onFragmentStateChange(UIConstants.EVENT_ITEM_CLICKED, it)
                            }
                        }
                        listAdapter.notifyDataSetChanged()
                    }

                    if( item == null ) hideDetailFragment()
                }

                refreshLayout.isRefreshing = false
            }
        })

        viewModel.readResult.observe(viewLifecycleOwner, Observer {
            if (refreshLayout.isRefreshing) {
                refreshLayout.isRefreshing = false
            }
        })

        viewModel.deleteResult.observe(viewLifecycleOwner, Observer {
            this.onDeleteComplete(it!!)
        })
    }

    /**
     * Checks if [item] exists in the list [items] based on the item id, which in offline is the read readLink,
     * while for online the primary key.
     */
    private fun containsItem(items: List<PhysicalInventoryCount>, item: PhysicalInventoryCount) : PhysicalInventoryCount? {
        return items.find { entry ->
            adapter?.getItemIdForPhysicalInventoryCount(entry) == adapter?.getItemIdForPhysicalInventoryCount(item)
        }
    }

    /** when no items return from server, hide the detail fragment on tablet */
    private fun hideDetailFragment() {
        currentActivity.supportFragmentManager.findFragmentByTag(UIConstants.DETAIL_FRAGMENT_TAG)?.let {
            currentActivity.supportFragmentManager.beginTransaction()
                .remove(it).commit()
        }
        secondaryToolbar?.let {
            it.menu.clear()
            it.title = ""
        }
        currentActivity.findViewById<ObjectHeader>(R.id.objectHeader)?.let {
            it.visibility = View.GONE
        }
    }

    /** Completion callback for delete operation  */
    private fun onDeleteComplete(result: OperationResult<PhysicalInventoryCount>) {
        progressBar?.let {
            it.visibility = View.INVISIBLE
        }
        viewModel.removeAllSelected()
        actionMode?.let {
            it.finish()
            isInActionMode = false
        }

        result.error?.let {
            handleDeleteError()
            return
        }
        refreshListData()
    }

    /** Handles the deletion error */
    private fun handleDeleteError() {
        showError(resources.getString(R.string.delete_failed_detail))
        refreshLayout.isRefreshing = false
    }

    /** sets up the refresh layout */
    private fun setupRefreshLayout() {
        refreshLayout = fragmentBinding.swiperefresh
        refreshLayout.setColorSchemeColors(UIConstants.FIORI_STANDARD_THEME_GLOBAL_DARK_BASE)
        refreshLayout.setProgressBackgroundColorSchemeColor(UIConstants.FIORI_STANDARD_THEME_BACKGROUND)
        refreshLayout.setOnRefreshListener(this::refreshListData)
    }

    /** Refreshes the list data */
    internal fun refreshListData() {
        navigationPropertyName?.let { _navigationPropertyName ->
            parentEntityData?.let { _parentEntityData ->
                viewModel.refresh(_parentEntityData as EntityValue, _navigationPropertyName)
            }
        } ?: run {
            viewModel.refresh()
        }
        adapter?.notifyDataSetChanged()
    }

    /** Sets the id for the selected item into view model */
    private fun setItemIdSelected(itemId: Int): PhysicalInventoryCount? {
        viewModel.observableItems.value?.let { physicalInventoryCountSet ->
            if (physicalInventoryCountSet.isNotEmpty()) {
                adapter?.let {
                    viewModel.inFocusId = it.getItemIdForPhysicalInventoryCount(physicalInventoryCountSet[itemId])
                    return physicalInventoryCountSet[itemId]
                }
            }
        }
        return null
    }

    /** Sets the detail image for the given [viewHolder] */
    private fun setDetailImage(viewHolder: PhysicalInventoryCountListAdapter.ViewHolder<ElementEntityitemListBinding>, physicalInventoryCountEntity: PhysicalInventoryCount?) {
        if (isInActionMode) {
            val drawable: Int = if (viewHolder.isSelected) {
                R.drawable.ic_check_circle_black_24dp
            } else {
                R.drawable.ic_uncheck_circle_black_24dp
            }
            viewHolder.objectCell.prepareDetailImageView().scaleType = ImageView.ScaleType.FIT_CENTER
            viewHolder.objectCell.detailImage = currentActivity.getDrawable(drawable)
        } else {
            if (!viewHolder.masterPropertyValue.isNullOrEmpty()) {
                viewHolder.objectCell.detailImageCharacter = viewHolder.masterPropertyValue?.substring(0, 1)
            } else {
                viewHolder.objectCell.detailImageCharacter = "?"
            }
        }
    }

    /**
     * Represents the listener to start the action mode. 
     */
    inner class OnActionModeStartClickListener(internal var holder: PhysicalInventoryCountListAdapter.ViewHolder<ElementEntityitemListBinding>) : View.OnClickListener, View.OnLongClickListener {

        override fun onClick(view: View) {
            onAnyKindOfClick()
        }

        override fun onLongClick(view: View): Boolean {
            return onAnyKindOfClick()
        }

        /** callback function for both normal and long click of an entity */
        private fun onAnyKindOfClick(): Boolean {
            val isNavigationDisabled = (activity as PhysicalInventoryCountSetActivity).isNavigationDisabled
            if (isNavigationDisabled) {
                Toast.makeText(activity, "Please save your changes first...", Toast.LENGTH_LONG).show()
            } else {
                if (!isInActionMode) {
                    actionMode = (currentActivity as AppCompatActivity).startSupportActionMode(PhysicalInventoryCountSetListActionMode())
                    adapter?.notifyDataSetChanged()
                }
                holder.isSelected = !holder.isSelected
            }
            return true
        }
    }

    /**
     * Represents list action mode.
     */
    inner class PhysicalInventoryCountSetListActionMode : ActionMode.Callback {
        override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
            isInActionMode = true
            fragmentBinding.fab?.let {
                it.hide()
            }
            //(currentActivity as PhysicalInventoryCountSetActivity).onSetActionModeFlag(isInActionMode)
            val inflater = actionMode.menuInflater
            inflater.inflate(R.menu.itemlist_view_options, menu)

            hideDetailFragment()
            return true
        }

        override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(actionMode: ActionMode, menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.update_item -> {
                    val physicalInventoryCountEntity = viewModel.getSelected(0)
                    if (viewModel.numberOfSelected() == 1 && physicalInventoryCountEntity != null) {
                        isInActionMode = false
                        actionMode.finish()
                        viewModel.setSelectedEntity(physicalInventoryCountEntity)
                        if(currentActivity.resources.getBoolean(R.bool.two_pane)) {
                            //make sure 'view' is under 'crt/update',
                            //so after done or back, the right panel has things to view
                            listener?.onFragmentStateChange(UIConstants.EVENT_ITEM_CLICKED, physicalInventoryCountEntity)
                        }
                        listener?.onFragmentStateChange(UIConstants.EVENT_EDIT_ITEM, physicalInventoryCountEntity)
                    }
                    true
                }
                R.id.delete_item -> {
                    listener?.onFragmentStateChange(UIConstants.EVENT_ASK_DELETE_CONFIRMATION,null)
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(actionMode: ActionMode) {
            isInActionMode = false
            if (!(navigationPropertyName != null && parentEntityData != null)) {
                fragmentBinding.fab?.let {
                    it.show()
                }
            }
            selectedItems.clear()
            viewModel.removeAllSelected()

            //if in big screen, make sure one item is selected.
            refreshListData()
        }
    }

    /**
    * List adapter to be used with RecyclerView. It contains the set of physicalInventoryCountSet.
    */
    inner class PhysicalInventoryCountListAdapter(private val context: Context, private val recyclerView: RecyclerView) : RecyclerView.Adapter<PhysicalInventoryCountListAdapter.ViewHolder<ElementEntityitemListBinding>>() {

        /** Entire list of PhysicalInventoryCount collection */
        private var physicalInventoryCountSet: MutableList<PhysicalInventoryCount> = ArrayList()

        /** Flag to indicate whether we have checked retained selected physicalInventoryCountSet */
        private var checkForSelectedOnCreate = false

        private lateinit var binding: ElementEntityitemListBinding

        init {
            setHasStableIds(true)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhysicalInventoryCountListAdapter.ViewHolder<ElementEntityitemListBinding> {
            binding = ElementEntityitemListBinding.inflate( LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun getItemCount(): Int {
            return physicalInventoryCountSet.size
        }

        override fun getItemId(position: Int): Long {
            return getItemIdForPhysicalInventoryCount(physicalInventoryCountSet[position])
        }

        override fun onBindViewHolder(holder: ViewHolder<ElementEntityitemListBinding>, position: Int) {
            checkForRetainedSelection()

            val physicalInventoryCountEntity = physicalInventoryCountSet[holder.bindingAdapterPosition]
            (physicalInventoryCountEntity.getOptionalValue(PhysicalInventoryCount.phyInvDocGUID))?.let {
                holder.masterPropertyValue = it.toString()
            }
            populateObjectCell(holder, physicalInventoryCountEntity)

            val isActive = getItemIdForPhysicalInventoryCount(physicalInventoryCountEntity) == viewModel.inFocusId
            if (isActive) {
                setItemIdSelected(holder.bindingAdapterPosition)
            }
            val isPhysicalInventoryCountSelected = viewModel.selectedContains(physicalInventoryCountEntity)
            setViewBackground(holder.objectCell, isPhysicalInventoryCountSelected, isActive)

            holder.itemView.setOnLongClickListener(OnActionModeStartClickListener(holder))
            setOnClickListener(holder, physicalInventoryCountEntity)

            setOnCheckedChangeListener(holder, physicalInventoryCountEntity)
            holder.isSelected = isPhysicalInventoryCountSelected
            setDetailImage(holder, physicalInventoryCountEntity)
        }

        /**
        * Check to see if there are an retained selected physicalInventoryCountEntity on start.
        * This situation occurs when a rotation with selected physicalInventoryCountSet is triggered by user.
        */
        private fun checkForRetainedSelection() {
            if (!checkForSelectedOnCreate) {
                checkForSelectedOnCreate = true
                if (viewModel.numberOfSelected() > 0) {
                    manageActionModeOnCheckedTransition()
                }
            }
        }

        /**
        * Computes a stable ID for each PhysicalInventoryCount object for use to locate the ViewHolder
        *
        * @param [physicalInventoryCountEntity] to get the items for
        * @return an ID based on the primary key of PhysicalInventoryCount
        */
        internal fun getItemIdForPhysicalInventoryCount(physicalInventoryCountEntity: PhysicalInventoryCount): Long {
            return physicalInventoryCountEntity.readLink.hashCode().toLong()
        }

        /**
        * Start Action Mode if it has not been started
        *
        * This is only called when long press action results in a selection. Hence action mode may not have been
        * started. Along with starting action mode, title will be set. If this is an additional selection, adjust title
        * appropriately.
        */
        private fun manageActionModeOnCheckedTransition() {
            if (actionMode == null) {
                actionMode = (activity as AppCompatActivity).startSupportActionMode(PhysicalInventoryCountSetListActionMode())
            }
            if (viewModel.numberOfSelected() > 1) {
                actionMode?.menu?.findItem(R.id.update_item)?.isVisible = false
            }
            actionMode?.title = viewModel.numberOfSelected().toString()
        }

        /**
        * This is called when one of the selected physicalInventoryCountSet has been de-selected
        *
        * On this event, we will determine if update action needs to be made visible or action mode should be
        * terminated (no more selected)
        */
        private fun manageActionModeOnUncheckedTransition() {
            when (viewModel.numberOfSelected()) {
                1 -> actionMode?.menu?.findItem(R.id.update_item)?.isVisible = true
                0 -> {
                    actionMode?.finish()
                    actionMode = null
                    return
                }
            }
            actionMode?.title = viewModel.numberOfSelected().toString()
        }

        private fun populateObjectCell(viewHolder: ViewHolder<ElementEntityitemListBinding>, physicalInventoryCountEntity: PhysicalInventoryCount) {

            val dataValue = physicalInventoryCountEntity.getOptionalValue(PhysicalInventoryCount.phyInvDocGUID)
            var masterPropertyValue: String? = null
            if (dataValue != null) {
                masterPropertyValue = dataValue.toString()
            }
            viewHolder.objectCell.apply {
                headline = masterPropertyValue
                setUseCutOut(false)
                setDetailImage(viewHolder, physicalInventoryCountEntity)
                subheadline = "Subheadline goes here"
                footnote = "Footnote goes here"
                when {
                physicalInventoryCountEntity.inErrorState -> setIcon(R.drawable.ic_error_state, 0, R.string.error_state)
                physicalInventoryCountEntity.isUpdated -> setIcon(R.drawable.ic_updated_state, 0, R.string.updated_state)
                physicalInventoryCountEntity.isLocal -> setIcon(R.drawable.ic_local_state, 0, R.string.local_state)
                else -> setIcon(R.drawable.ic_download_state, 0, R.string.download_state)
                }
                setIcon(R.drawable.default_dot, 1, R.string.attachment_item_content_desc)
            }
        }

        private fun processClickAction(viewHolder: ViewHolder<ElementEntityitemListBinding>, physicalInventoryCountEntity: PhysicalInventoryCount) {
            resetPreviouslyClicked()
            setViewBackground(viewHolder.objectCell, false, true)
            viewModel.inFocusId = getItemIdForPhysicalInventoryCount(physicalInventoryCountEntity)
        }

        /**
        * Attempt to locate previously clicked view and reset its background
        * Reset view model's inFocusId
        */
        private fun resetPreviouslyClicked() {
            (recyclerView.findViewHolderForItemId(viewModel.inFocusId) as ViewHolder<ElementEntityitemListBinding>?)?.let {
                setViewBackground(it.objectCell, it.isSelected, false)
            } ?: run {
                viewModel.refresh()
            }
        }

        /**
        * If there are selected physicalInventoryCountSet via long press, clear them as click and long press are mutually exclusive
        * In addition, since we are clearing all selected physicalInventoryCountSet via long press, finish the action mode.
        */
        private fun resetSelected() {
            if (viewModel.numberOfSelected() > 0) {
                viewModel.removeAllSelected()
                if (actionMode != null) {
                    actionMode?.finish()
                    actionMode = null
                }
            }
        }

        /**
        * Set up checkbox value and visibility based on physicalInventoryCountEntity selection status
        *
        * @param [checkBox] to set
        * @param [isPhysicalInventoryCountSelected] true if physicalInventoryCountEntity is selected via long press action
        */
        private fun setCheckBox(checkBox: CheckBox, isPhysicalInventoryCountSelected: Boolean) {
            checkBox.isChecked = isPhysicalInventoryCountSelected
        }

        /**
        * Use DiffUtil to calculate the difference and dispatch them to the adapter
        * Note: Please use background thread for calculation if the list is large to avoid blocking main thread
        */
        @WorkerThread
        fun setItems(currentPhysicalInventoryCountSet: List<PhysicalInventoryCount>) {
            if (physicalInventoryCountSet.isEmpty()) {
                physicalInventoryCountSet = java.util.ArrayList(currentPhysicalInventoryCountSet)
                notifyItemRangeInserted(0, currentPhysicalInventoryCountSet.size)
            } else {
                val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                    override fun getOldListSize(): Int {
                        return physicalInventoryCountSet.size
                    }

                    override fun getNewListSize(): Int {
                        return currentPhysicalInventoryCountSet.size
                    }

                    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        return physicalInventoryCountSet[oldItemPosition].readLink == currentPhysicalInventoryCountSet[newItemPosition].readLink
                    }

                    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        val physicalInventoryCountEntity = physicalInventoryCountSet[oldItemPosition]
                        return !physicalInventoryCountEntity.isUpdated && currentPhysicalInventoryCountSet[newItemPosition] == physicalInventoryCountEntity
                    }
                })
                physicalInventoryCountSet.clear()
                physicalInventoryCountSet.addAll(currentPhysicalInventoryCountSet)
                result.dispatchUpdatesTo(this)
            }
        }

        /**
        * Set ViewHolder's CheckBox onCheckedChangeListener
        *
        * @param [holder] to set
        * @param [physicalInventoryCountEntity] associated with this ViewHolder
        */
        private fun setOnCheckedChangeListener(holder: ViewHolder<ElementEntityitemListBinding>, physicalInventoryCountEntity: PhysicalInventoryCount) {
            holder.checkBox.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    //(currentActivity as PhysicalInventoryCountSetActivity).onUnderDeletion(physicalInventoryCountEntity, true)
                    viewModel.addSelected(physicalInventoryCountEntity)
                    manageActionModeOnCheckedTransition()
                    resetPreviouslyClicked()
                } else {
                    //(currentActivity as PhysicalInventoryCountSetActivity).onUnderDeletion(physicalInventoryCountEntity, false)
                    viewModel.removeSelected(physicalInventoryCountEntity)
                    manageActionModeOnUncheckedTransition()
                }
                setViewBackground(holder.objectCell, viewModel.selectedContains(physicalInventoryCountEntity), false)
                setDetailImage(holder, physicalInventoryCountEntity)
            }
        }

        /**
        * Set ViewHolder's view onClickListener
        *
        * @param [holder] to set
        * @param [physicalInventoryCountEntity] associated with this ViewHolder
        */
        private fun setOnClickListener(holder: ViewHolder<ElementEntityitemListBinding>, physicalInventoryCountEntity: PhysicalInventoryCount) {
            holder.itemView.setOnClickListener { view ->
                val isNavigationDisabled = (currentActivity as PhysicalInventoryCountSetActivity).isNavigationDisabled
                if( !isNavigationDisabled ) {
                    resetSelected()
                    resetPreviouslyClicked()
                    processClickAction(holder, physicalInventoryCountEntity)
                    viewModel.setSelectedEntity(physicalInventoryCountEntity)
                    listener?.onFragmentStateChange(UIConstants.EVENT_ITEM_CLICKED, physicalInventoryCountEntity)
                } else {
                    Toast.makeText(currentActivity, "Please save your changes first...", Toast.LENGTH_LONG).show()
                }
            }
        }

        /**
        * Set background of view to indicate physicalInventoryCountEntity selection status
        * Selected and Active are mutually exclusive. Only one can be true
        *
        * @param [view]
        * @param [isPhysicalInventoryCountSelected] - true if physicalInventoryCountEntity is selected via long press action
        * @param [isActive]           - true if physicalInventoryCountEntity is selected via click action
        */
        private fun setViewBackground(view: View, isPhysicalInventoryCountSelected: Boolean, isActive: Boolean) {
            val isMasterDetailView = currentActivity.resources.getBoolean(R.bool.two_pane)
            if (isPhysicalInventoryCountSelected) {
                view.background = ContextCompat.getDrawable(context, R.drawable.list_item_selected)
            } else if (isActive && isMasterDetailView && !isInActionMode) {
                view.background = ContextCompat.getDrawable(context, R.drawable.list_item_active)
            } else {
                view.background = ContextCompat.getDrawable(context, R.drawable.list_item_default)
            }
        }

        /**
        * ViewHolder for RecyclerView.
        * Each view has a Fiori ObjectCell and a checkbox (used by long press)
        */
        inner class ViewHolder<VB: ElementEntityitemListBinding>(private val viewBinding: VB) : RecyclerView.ViewHolder(viewBinding.root) {

            var isSelected = false
                set(selected) {
                    field = selected
                    checkBox.isChecked = selected
                }

            var masterPropertyValue: String? = null

            /** Fiori ObjectCell to display physicalInventoryCountEntity in list */
            val objectCell: ObjectCell = viewBinding.content

            /** Checkbox for long press selection */
            val checkBox: CheckBox = viewBinding.cbx

            override fun toString(): String {
                return super.toString() + " '" + objectCell.description + "'"
            }
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(PhysicalInventoryCountSetActivity::class.java)
    }
}
