package com.example.espacebenevole.ui.gallery


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.espacebenevole.databinding.FragmentGalleryBinding
import com.example.espacebenevole.ui.home.Event
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import org.json.JSONArray
import org.json.JSONException
import org.threeten.bp.DateTimeException
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeParseException


class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        Toast.makeText(context, "HEEEEY", Toast.LENGTH_SHORT).show()
        fetchEvents()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fetchEvents() {
        val queue = Volley.newRequestQueue(context)
        val url = "https://projet-annuel-paoli.koyeb.app/api/index.php/volunteer/planning"

        val stringRequest = object : StringRequest(
            Request.Method.GET, url,
            Response.Listener<String> { response ->
                val eventsArray = JSONArray(response)
                val eventsList = eventsArray.toEventList()
                Toast.makeText(context, response, Toast.LENGTH_LONG).show()
                updateCalendar(eventsList)
            },
            Response.ErrorListener { error ->
                Toast.makeText(context, "Failed to fetch events: ${error.toString()}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                val sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", AppCompatActivity.MODE_PRIVATE)
                val token = sharedPreferences.getString("AuthToken", "")
                return mapOf("auth" to token!!)
            }
        }
        queue.add(stringRequest)
    }

    /*private fun fetchEvents2() {

        Toast.makeText(context, "HEEEEY", Toast.LENGTH_SHORT).show()
        val jsonData = """
        [
            {
                "id": 1,
                "title": "Charity Bake Sale",
                "startDate": "2024-09-10 08:00:00",
                "endDate": "2024-09-12 12:00:00",
                "address": "123 Cupcake Way"
            },
            {
                "id": 2,
                "title": "Book Drive",
                "startDate": "2024-12-10 09:00:00",
                "endDate": "2024-12-12 15:00:00",
                "address": "456 Library Ave"
            }
        ]
    """
        try {
            val eventsArray = JSONArray(jsonData)
            val eventsList = eventsArray.toEventList()
            Toast.makeText(context, "Events loaded from JSON.", Toast.LENGTH_LONG).show()

            updateCalendar(eventsList)
        } catch (e: JSONException) {
            Toast.makeText(context, "Failed to parse JSON data: ${e.toString()}", Toast.LENGTH_LONG).show()
        }
    }*/



    private fun parseDateTime(dateTimeStr: String): LocalDateTime? {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return try {
            LocalDateTime.parse(dateTimeStr, formatter).also {
                // After parsing, verify the date is valid.
                verifyDate(it.toLocalDate())
            }
        } catch (e: DateTimeParseException) {
            Log.e("DateParsing", "Failed to parse date: $dateTimeStr", e)
            null
        } catch (e: DateTimeException) {
            Log.e("DateParsing", "Invalid date found: $dateTimeStr", e)
            null
        }
    }

    private fun verifyDate(date: LocalDate) {
        val year = date.year
        val month = date.monthValue
        val day = date.dayOfMonth
        if (day > YearMonth.of(year, month).lengthOfMonth()) {
            throw DateTimeException("Invalid date '$month $day'")
        }
    }


    private fun updateCalendar(eventsList: List<Event>) {
        eventsList.forEach { event ->
            val startDateTime = parseDateTime(event.startDate)
            val endDateTime = parseDateTime(event.endDate)
            Log.d("CalendarUpdate", "Processing event: ${event.title}, Start Date: ${startDateTime}, End Date: ${endDateTime}")

            if (startDateTime != null && endDateTime != null) {
                val startDate = startDateTime.toLocalDate()
                val endDate = endDateTime.toLocalDate()
                binding.calendarView.addDecorator(EventDecorator(startDate, endDate))
            } else {
                Toast.makeText(context, "Invalid date format in event data", Toast.LENGTH_SHORT).show()
            }
        }
    }






    inner class EventDecorator(private val startDate: LocalDate, private val endDate: LocalDate) : DayViewDecorator {
        override fun shouldDecorate(day: CalendarDay): Boolean {
            val year = day.year
            val month = day.month  // Utilisez directement le mois sans ajouter 1

            val dayDate = try {
                LocalDate.of(year, month, day.day)
            } catch (e: DateTimeException) {
                Log.e("EventDecorator", "Invalid date attempted: $year-$month-${day.day}", e)
                return false  // Retourne false si la date est invalide
            }

            return dayDate >= startDate && dayDate <= endDate
        }

        override fun decorate(view: DayViewFacade) {
            view.addSpan(DotSpan(5f, ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)))
        }
    }






    fun JSONArray.toEventList(): List<Event> {
        val list = mutableListOf<Event>()
        for (i in 0 until length()) {
            val jsonObject = getJSONObject(i)
            list.add(
                Event(
                    id = jsonObject.getInt("id"),
                    title = jsonObject.getString("title"),
                    startDate = jsonObject.getString("startDate"),
                    endDate = jsonObject.getString("endDate"),
                    address = jsonObject.getString("address")
                )
            )
        }
        return list
    }
}