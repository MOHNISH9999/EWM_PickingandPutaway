package com.company.ewm_pickingandputaway.mdui

import com.company.ewm_pickingandputaway.app.SAPWizardApplication

import com.sap.cloud.mobile.flowv2.core.DialogHelper
import com.sap.cloud.mobile.flowv2.core.Flow
import com.sap.cloud.mobile.flowv2.core.FlowContextRegistry
import com.sap.cloud.mobile.flowv2.model.FlowType
import com.sap.cloud.mobile.flowv2.securestore.UserSecureStoreDelegate
import androidx.preference.PreferenceManager
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.company.ewm_pickingandputaway.service.*
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.*
import android.widget.ArrayAdapter
import android.content.Context
import android.content.Intent
import java.util.ArrayList
import java.util.HashMap
import com.company.ewm_pickingandputaway.app.WelcomeActivity
import com.company.ewm_pickingandputaway.databinding.ActivityEntitySetListBinding
import com.company.ewm_pickingandputaway.databinding.ElementEntitySetListBinding
import com.company.ewm_pickingandputaway.mdui.physicalinventorycountset.PhysicalInventoryCountSetActivity
import com.company.ewm_pickingandputaway.mdui.repackhuitemset.RepackHuItemSetActivity
import org.slf4j.LoggerFactory
import com.company.ewm_pickingandputaway.R

/*
 * An activity to display the list of all entity types from the OData service
 */
class EntitySetListActivity : AppCompatActivity() {
    private val entitySetNames = ArrayList<String>()
    private val entitySetNameMap = HashMap<String, EntitySetName>()
    private lateinit var binding: ActivityEntitySetListBinding

    private var syncItem: MenuItem? = null

    enum class EntitySetName constructor(val entitySetName: String, val titleId: Int, val iconId: Int) {
        PhysicalInventoryCountSet("PhysicalInventoryCountSet", R.string.eset_physicalinventorycountset,
            BLUE_ANDROID_ICON),
        RepackHuItemSet("RepackHuItemSet", R.string.eset_repackhuitemset,
            WHITE_ANDROID_ICON),
        SAPCurrencies("SAPCurrencies", R.string.eset_sapcurrencies,
        BLUE_ANDROID_ICON),
        SAPUnitsOfMeasure("SAPUnitsOfMeasure", R.string.eset_sapunitsofmeasure,
        WHITE_ANDROID_ICON),
        WarehouseOrder("WarehouseOrder", R.string.eset_warehouseorder,
        BLUE_ANDROID_ICON),
        WarehouseTask("WarehouseTask", R.string.eset_warehousetask,
        WHITE_ANDROID_ICON),
        WarehouseTaskExceptionCode("WarehouseTaskExceptionCode", R.string.eset_warehousetaskexceptioncode,
        BLUE_ANDROID_ICON)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //navigate to launch screen if SAPServiceManager or OfflineOdataProvider is not initialized
        navForInitialize()
        binding = ActivityEntitySetListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbar = findViewById<Toolbar>(R.id.toolbar) // to avoid ambiguity
        setSupportActionBar(toolbar)

        entitySetNames.clear()
        entitySetNameMap.clear()
        for (entitySet in EntitySetName.values()) {
            val entitySetTitle = resources.getString(entitySet.titleId)
            entitySetNames.add(entitySetTitle)
            entitySetNameMap[entitySetTitle] = entitySet
        }

        val listView = binding.entityList
        val adapter = EntitySetListAdapter(this, R.layout.element_entity_set_list, entitySetNames)

        listView.adapter = adapter

        listView.setOnItemClickListener listView@{ _, _, position, _ ->
            val entitySetName = entitySetNameMap[adapter.getItem(position)!!]
            val context = this@EntitySetListActivity
            val intent: Intent = when (entitySetName) {
                EntitySetName.PhysicalInventoryCountSet -> Intent(context, PhysicalInventoryCountSetActivity::class.java)
                EntitySetName.RepackHuItemSet -> Intent(context, RepackHuItemSetActivity::class.java)
                else -> return@listView
            }
            context.startActivity(intent)
        }
    }

    inner class EntitySetListAdapter internal constructor(context: Context, resource: Int, entitySetNames: List<String>)
                    : ArrayAdapter<String>(context, resource, entitySetNames) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view = convertView
            var viewBind :ElementEntitySetListBinding
            val entitySetName = entitySetNameMap[getItem(position)!!]
            if (view == null) {
                viewBind = ElementEntitySetListBinding.inflate(LayoutInflater.from(context), parent, false)
                view = viewBind.root
            } else {
                viewBind = ElementEntitySetListBinding.bind(view)
            }
            val entitySetCell = viewBind.entitySetName
            entitySetCell.headline = entitySetName?.titleId?.let {
                context.resources.getString(it)
            }
            entitySetName?.iconId?.let { entitySetCell.setDetailImage(it) }
            return view
        }
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.entity_set_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.menu_delete_registration)?.isEnabled =
            UserSecureStoreDelegate.getInstance().getRuntimeMultipleUserModeAsync() == true
        menu?.findItem(R.id.menu_delete_registration)?.isVisible =
            UserSecureStoreDelegate.getInstance().getRuntimeMultipleUserModeAsync() == true
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        LOGGER.debug("onOptionsItemSelected: " + item.title)
        return when (item.itemId) {
            R.id.menu_settings -> {
                LOGGER.debug("settings screen menu item selected.")
                Intent(this, SettingsActivity::class.java).also {
                    this.startActivity(it)
                }
                true
            }
            R.id.menu_sync -> {
                syncItem = item
                synchronize()
                true
            }
            R.id.menu_logout -> {
                Flow.start(this, FlowContextRegistry.flowContext.copy(
                    flowType = FlowType.LOGOUT,
                )) { _, resultCode, _ ->
                    if (resultCode == RESULT_OK) {
                        Intent(this, WelcomeActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            startActivity(this)
                        }
                    }
                }
                true
            }
            R.id.menu_delete_registration -> {
                DialogHelper.ErrorDialogFragment(
                    message = getString(R.string.delete_registration_warning),
                    title = getString(R.string.dialog_warn_title),
                    positiveButtonCaption = getString(R.string.confirm_yes),
                    negativeButtonCaption = getString(R.string.cancel),
                    positiveAction = {
                        Flow.start(this, FlowContextRegistry.flowContext.copy(
                            flowType = FlowType.DEL_REGISTRATION
                        )) { _, resultCode, _ ->
                            if (resultCode == RESULT_OK) {
                                PreferenceManager.getDefaultSharedPreferences(this)
                                    .edit()
                                    .putBoolean(OfflineWorkerUtil.PREF_DELETE_REGISTRATION, true)
                                    .apply()
                                Intent(this, WelcomeActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    startActivity(this)
                                }
                            }
                        }
                    }
                ).apply {
                    isCancelable = false
                    show(supportFragmentManager, this@EntitySetListActivity.getString(R.string.delete_registration))
                }
                true
            }
            else -> false
        }
    }

    private fun navForInitialize() {
        if (OfflineWorkerUtil.offlineODataProvider == null) {
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        OfflineWorkerUtil.syncRequest?.let {
            updateProgressForSync()
        }
    }

    override fun onStop() {
        super.onStop()
        OfflineWorkerUtil.syncRequest?.let {
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putBoolean(OfflineWorkerUtil.PREF_FOREGROUND_SERVICE, true)
                .apply()
        }
    }

    private fun synchronize() {
        OfflineWorkerUtil.sync(applicationContext)
        updateProgressForSync()
    }

    private fun updateProgressForSync() {
        syncItem?.isEnabled = false
        OfflineWorkerUtil.addProgressListener(progressListener)
        binding.syncDeterminate.visibility = View.VISIBLE
        WorkManager.getInstance(applicationContext)
            .getWorkInfoByIdLiveData(OfflineWorkerUtil.syncRequest!!.id)
            .observe(this, { workInfo ->
                if (workInfo != null && workInfo.state.isFinished) {
                    syncItem?.isEnabled = true
                    OfflineWorkerUtil.removeProgressListener(progressListener)
                    OfflineWorkerUtil.resetSyncRequest()
                    with(binding.syncDeterminate) {
                        visibility = View.INVISIBLE
                        progress = 0
                    }
                    when(workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            LOGGER.info("Offline sync done.")
                        }
                        WorkInfo.State.FAILED -> {
                            DialogHelper(this@EntitySetListActivity).showOKOnlyDialog(
                                fragmentManager = supportFragmentManager,
                                message = workInfo.outputData.getString(OfflineWorkerUtil.OUTPUT_ERROR_DETAIL) ?: getString(R.string.synchronize_failure_detail)
                            )
                        }
                        else -> {
                            //do nothing
                        }
                    }
                }
            })
    }

    private val progressListener = object : OfflineProgressListener() {
        override val workerType = WorkerType.SYNC

        override fun updateProgress(currentStep: Int, totalSteps: Int) {
            with(binding.syncDeterminate) {
                max = totalSteps
                progress = currentStep
            }
        }

        override fun getStartPoint(): Int {
            return OfflineSyncWorker.startPointForSync
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(EntitySetListActivity::class.java)
        private const val BLUE_ANDROID_ICON = R.drawable.ic_android_blue
        private const val WHITE_ANDROID_ICON = R.drawable.ic_android_white
    }
}
