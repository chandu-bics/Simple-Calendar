package com.simplemobiletools.calendar.fragments

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.EventActivity
import com.simplemobiletools.calendar.activities.SimpleActivity
import com.simplemobiletools.calendar.adapters.EventListAdapter
import com.simplemobiletools.calendar.extensions.*
import com.simplemobiletools.calendar.helpers.EVENT_ID
import com.simplemobiletools.calendar.helpers.EVENT_OCCURRENCE_TS
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.calendar.models.ListEvent
import com.simplemobiletools.commons.extensions.beGoneIf
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import kotlinx.android.synthetic.main.fragment_event_list.view.*
import org.joda.time.DateTime
import java.util.*

class EventListFragment : Fragment(), RefreshRecyclerViewListener {
    private var mEvents: List<Event> = ArrayList()
    private var prevEventsHash = 0
    private var lastHash = 0
    lateinit var mView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.fragment_event_list, container, false)
        val placeholderText = String.format(getString(R.string.two_string_placeholder), "${getString(R.string.no_upcoming_events)}\n", getString(R.string.add_some_events))
        mView.calendar_empty_list_placeholder.text = placeholderText
        return mView
    }

    override fun onResume() {
        super.onResume()
        checkEvents()
    }

    private fun checkEvents() {
        val fromTS = DateTime().seconds() - context!!.config.displayPastEvents * 60
        val toTS = DateTime().plusYears(1).seconds()
        context!!.dbHelper.getEvents(fromTS, toTS) {
            receivedEvents(it)
        }
    }

    private fun receivedEvents(events: MutableList<Event>) {
        if (context == null || activity == null)
            return

        val newHash = events.hashCode()
        if (newHash == lastHash) {
            return
        }
        lastHash = newHash

        val filtered = context!!.getFilteredEvents(events)
        val hash = filtered.hashCode()
        if (prevEventsHash == hash)
            return

        prevEventsHash = hash
        mEvents = filtered
        val listItems = context!!.getEventListItems(mEvents)

        val eventsAdapter = EventListAdapter(activity as SimpleActivity, listItems, true, this, mView.calendar_events_list) {
            if (it is ListEvent) {
                editEvent(it)
            }
        }

        activity?.runOnUiThread {
            mView.calendar_events_list.adapter = eventsAdapter
            checkPlaceholderVisibility()
        }
    }

    private fun checkPlaceholderVisibility() {
        mView.calendar_empty_list_placeholder.beVisibleIf(mEvents.isEmpty())
        mView.calendar_events_list.beGoneIf(mEvents.isEmpty())
        if (activity != null)
            mView.calendar_empty_list_placeholder.setTextColor(activity!!.config.textColor)
    }

    private fun editEvent(event: ListEvent) {
        Intent(context, EventActivity::class.java).apply {
            putExtra(EVENT_ID, event.id)
            putExtra(EVENT_OCCURRENCE_TS, event.startTS)
            startActivity(this)
        }
    }

    override fun refreshItems() {
        checkEvents()
    }
}
