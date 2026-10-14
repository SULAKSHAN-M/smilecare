package com.smilecare.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

private val Navy = Color(0xFF102B4E)
private val Teal = Color(0xFF0AAFB5)
private val TealDark = Color(0xFF078F95)
private val Aqua = Color(0xFFE9FBFB)
private val SurfaceBlue = Color(0xFFF4FAFC)
private val SoftBlue = Color(0xFFEAF4FF)
private val SoftPink = Color(0xFFFFEEF5)
private val SoftPurple = Color(0xFFF1EEFF)
private val SoftGreen = Color(0xFFEBFAF2)
private val SoftOrange = Color(0xFFFFF4E6)
private val Muted = Color(0xFF71859A)
private val Danger = Color(0xFFE74C63)
private val Success = Color(0xFF2DAA6B)
private val Warning = Color(0xFFE59C2C)

private const val DOCTOR_HD = "https://images.pexels.com/photos/31043313/pexels-photo-31043313.jpeg?auto=compress&cs=tinysrgb&w=1600"
private const val PATIENT_HD = "https://images.pexels.com/photos/20603310/pexels-photo-20603310.jpeg?auto=compress&cs=tinysrgb&w=1200"
private const val TREATMENT_HD = "https://images.pexels.com/photos/3881221/pexels-photo-3881221.jpeg?auto=compress&cs=tinysrgb&w=1800"
private const val SMILE_HD = "https://images.pexels.com/photos/7479536/pexels-photo-7479536.jpeg?auto=compress&cs=tinysrgb&w=1600"

private enum class Role { PATIENT, DOCTOR }
private enum class AppointmentStatus { PENDING, CONFIRMED, COMPLETED, CANCELLED }
private enum class AppScreen {
    AUTH,
    PATIENT_HOME, PATIENT_BOOK, PATIENT_APPOINTMENTS, PATIENT_HISTORY, PATIENT_PROFILE, PATIENT_NOTIFICATIONS, PATIENT_DOCTOR,
    DOCTOR_HOME, DOCTOR_APPOINTMENTS, DOCTOR_PATIENTS, DOCTOR_SCHEDULE, DOCTOR_PROFILE, DOCTOR_NOTIFICATIONS,
    APPOINTMENT_DETAIL, CHAT
}

private data class Appointment(
    val id: String,
    val patient: String,
    val doctor: String,
    val service: String,
    val date: String,
    val time: String,
    val status: AppointmentStatus,
    val fee: Int = 500,
    val notes: String = "",
    val prescription: String = ""
)

private data class ChatMessage(
    val id: String,
    val sender: Role,
    val text: String,
    val time: String
)

private data class Notice(
    val id: String,
    val target: Role,
    val title: String,
    val text: String,
    val time: String
)

private class SmileCareStore(context: Context) {
    private val prefs = context.getSharedPreferences("smilecare_store_v2", Context.MODE_PRIVATE)

    var patientName by mutableStateOf(prefs.getString("patient_name", "Alishba Falaq") ?: "Alishba Falaq")
        private set
    var patientEmail by mutableStateOf(prefs.getString("patient_email", "patient@smilecare.app") ?: "patient@smilecare.app")
        private set
    private var patientPassword: String = prefs.getString("patient_password", "123456") ?: "123456"

    var doctorName by mutableStateOf(prefs.getString("doctor_name", "Dr. Farhana Akter") ?: "Dr. Farhana Akter")
        private set
    var doctorEmail by mutableStateOf(prefs.getString("doctor_email", "doctor@smilecare.app") ?: "doctor@smilecare.app")
        private set
    private var doctorPassword: String = prefs.getString("doctor_password", "123456") ?: "123456"

    val appointments = mutableStateListOf<Appointment>()
    val messages = mutableStateListOf<ChatMessage>()
    val notices = mutableStateListOf<Notice>()
    val availability = mutableStateListOf<String>()

    init {
        loadAppointments()
        loadMessages()
        loadNotices()
        loadAvailability()
    }

    fun signIn(role: Role, email: String, password: String): Boolean {
        val cleanEmail = email.trim().lowercase(Locale.getDefault())
        return when (role) {
            Role.PATIENT ->
                (cleanEmail == "patient@smilecare.app" && password == "123456") ||
                    (cleanEmail == patientEmail.lowercase(Locale.getDefault()) && password == patientPassword)
            Role.DOCTOR ->
                (cleanEmail == "doctor@smilecare.app" && password == "123456") ||
                    (cleanEmail == doctorEmail.lowercase(Locale.getDefault()) && password == doctorPassword)
        }
    }

    fun register(role: Role, name: String, email: String, password: String) {
        if (role == Role.PATIENT) {
            patientName = name.trim()
            patientEmail = email.trim()
            patientPassword = password
            prefs.edit()
                .putString("patient_name", patientName)
                .putString("patient_email", patientEmail)
                .putString("patient_password", patientPassword)
                .apply()
        } else {
            doctorName = name.trim()
            doctorEmail = email.trim()
            doctorPassword = password
            prefs.edit()
                .putString("doctor_name", doctorName)
                .putString("doctor_email", doctorEmail)
                .putString("doctor_password", doctorPassword)
                .apply()
        }
    }

    fun updateProfile(role: Role, name: String, email: String) {
        if (role == Role.PATIENT) {
            patientName = name.trim()
            patientEmail = email.trim()
            prefs.edit().putString("patient_name", patientName).putString("patient_email", patientEmail).apply()
            for (i in appointments.indices) {
                appointments[i] = appointments[i].copy(patient = patientName)
            }
        } else {
            doctorName = name.trim()
            doctorEmail = email.trim()
            prefs.edit().putString("doctor_name", doctorName).putString("doctor_email", doctorEmail).apply()
            for (i in appointments.indices) {
                appointments[i] = appointments[i].copy(doctor = doctorName)
            }
        }
        saveAppointments()
    }

    fun addAppointment(service: String, date: String, time: String): String {
        val id = UUID.randomUUID().toString()
        appointments.add(
            0,
            Appointment(
                id = id,
                patient = patientName,
                doctor = doctorName,
                service = service,
                date = date,
                time = time,
                status = AppointmentStatus.PENDING,
                fee = serviceFee(service)
            )
        )
        addNotice(Role.DOCTOR, "New appointment request", "$patientName requested $service on $date at $time.")
        addNotice(Role.PATIENT, "Request sent", "Your $service request was sent to $doctorName for approval.")
        saveAppointments()
        return id
    }

    fun rescheduleAppointment(id: String, service: String, date: String, time: String) {
        val index = appointments.indexOfFirst { it.id == id }
        if (index < 0) return
        appointments[index] = appointments[index].copy(
            service = service,
            date = date,
            time = time,
            status = AppointmentStatus.PENDING,
            fee = serviceFee(service)
        )
        addNotice(Role.DOCTOR, "Reschedule request", "$patientName requested a new time: $date at $time.")
        addNotice(Role.PATIENT, "Reschedule submitted", "Your new appointment time is waiting for doctor approval.")
        saveAppointments()
    }

    fun updateStatus(id: String, status: AppointmentStatus) {
        val index = appointments.indexOfFirst { it.id == id }
        if (index < 0) return
        val current = appointments[index]
        appointments[index] = current.copy(status = status)
        when (status) {
            AppointmentStatus.CONFIRMED -> addNotice(Role.PATIENT, "Appointment confirmed", "${current.service} on ${current.date} at ${current.time} has been confirmed.")
            AppointmentStatus.CANCELLED -> {
                addNotice(Role.PATIENT, "Appointment cancelled", "${current.service} on ${current.date} has been cancelled.")
                addNotice(Role.DOCTOR, "Appointment cancelled", "${current.patient} cancelled ${current.service} on ${current.date}.")
            }
            AppointmentStatus.COMPLETED -> addNotice(Role.PATIENT, "Treatment completed", "Your ${current.service} visit is complete. Notes and prescription are now available.")
            AppointmentStatus.PENDING -> Unit
        }
        saveAppointments()
    }

    fun completeAppointment(id: String, notes: String, prescription: String) {
        val index = appointments.indexOfFirst { it.id == id }
        if (index < 0) return
        appointments[index] = appointments[index].copy(
            status = AppointmentStatus.COMPLETED,
            notes = notes.trim(),
            prescription = prescription.trim()
        )
        addNotice(Role.PATIENT, "Report ready", "Your treatment report for ${appointments[index].service} is ready.")
        saveAppointments()
    }

    fun findAppointment(id: String?): Appointment? = appointments.firstOrNull { it.id == id }

    fun isSlotBooked(date: String, time: String, exceptId: String? = null): Boolean = appointments.any {
        it.id != exceptId && it.date == date && it.time == time && it.status != AppointmentStatus.CANCELLED
    }

    fun addMessage(sender: Role, text: String) {
        if (text.isBlank()) return
        messages.add(ChatMessage(UUID.randomUUID().toString(), sender, text.trim(), currentTime()))
        addNotice(
            if (sender == Role.PATIENT) Role.DOCTOR else Role.PATIENT,
            "New message",
            if (sender == Role.PATIENT) "$patientName sent you a message." else "$doctorName sent you a message."
        )
        saveMessages()
    }

    fun toggleAvailability(slot: String) {
        if (availability.contains(slot)) availability.remove(slot) else availability.add(slot)
        saveAvailability()
    }

    fun resetDemoData() {
        appointments.clear()
        messages.clear()
        notices.clear()
        availability.clear()
        seedAppointments()
        seedMessages()
        seedNotices()
        availability.addAll(defaultSlots())
        saveAppointments(); saveMessages(); saveNotices(); saveAvailability()
    }

    private fun addNotice(target: Role, title: String, text: String) {
        notices.add(0, Notice(UUID.randomUUID().toString(), target, title, text, currentTime()))
        if (notices.size > 30) notices.removeLast()
        saveNotices()
    }

    private fun loadAppointments() {
        val raw = prefs.getString("appointments", null)
        if (raw.isNullOrBlank()) {
            seedAppointments(); saveAppointments(); return
        }
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                appointments.add(
                    Appointment(
                        id = o.optString("id"),
                        patient = o.optString("patient", patientName),
                        doctor = o.optString("doctor", doctorName),
                        service = o.optString("service"),
                        date = o.optString("date"),
                        time = o.optString("time"),
                        status = runCatching { AppointmentStatus.valueOf(o.optString("status")) }.getOrDefault(AppointmentStatus.PENDING),
                        fee = o.optInt("fee", 500),
                        notes = o.optString("notes"),
                        prescription = o.optString("prescription")
                    )
                )
            }
        } catch (_: Exception) {
            appointments.clear(); seedAppointments(); saveAppointments()
        }
    }

    private fun saveAppointments() {
        val array = JSONArray()
        appointments.forEach { a ->
            array.put(JSONObject().apply {
                put("id", a.id); put("patient", a.patient); put("doctor", a.doctor); put("service", a.service)
                put("date", a.date); put("time", a.time); put("status", a.status.name); put("fee", a.fee)
                put("notes", a.notes); put("prescription", a.prescription)
            })
        }
        prefs.edit().putString("appointments", array.toString()).apply()
    }

    private fun loadMessages() {
        val raw = prefs.getString("messages", null)
        if (raw.isNullOrBlank()) {
            seedMessages(); saveMessages(); return
        }
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                messages.add(ChatMessage(o.optString("id"), Role.valueOf(o.optString("sender")), o.optString("text"), o.optString("time")))
            }
        } catch (_: Exception) {
            messages.clear(); seedMessages(); saveMessages()
        }
    }

    private fun saveMessages() {
        val array = JSONArray()
        messages.forEach { m -> array.put(JSONObject().apply { put("id", m.id); put("sender", m.sender.name); put("text", m.text); put("time", m.time) }) }
        prefs.edit().putString("messages", array.toString()).apply()
    }

    private fun loadNotices() {
        val raw = prefs.getString("notices", null)
        if (raw.isNullOrBlank()) {
            seedNotices(); saveNotices(); return
        }
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                notices.add(Notice(o.optString("id"), Role.valueOf(o.optString("target")), o.optString("title"), o.optString("text"), o.optString("time")))
            }
        } catch (_: Exception) {
            notices.clear(); seedNotices(); saveNotices()
        }
    }

    private fun saveNotices() {
        val array = JSONArray()
        notices.forEach { n -> array.put(JSONObject().apply { put("id", n.id); put("target", n.target.name); put("title", n.title); put("text", n.text); put("time", n.time) }) }
        prefs.edit().putString("notices", array.toString()).apply()
    }

    private fun loadAvailability() {
        val saved = prefs.getStringSet("availability", null)
        if (saved.isNullOrEmpty()) availability.addAll(defaultSlots()) else availability.addAll(saved.sortedBy { defaultSlots().indexOf(it) })
    }

    private fun saveAvailability() {
        prefs.edit().putStringSet("availability", availability.toSet()).apply()
    }

    private fun seedAppointments() {
        val dates = nextBookingDates()
        appointments.addAll(
            listOf(
                Appointment("seed-1", patientName, doctorName, "Teeth Cleaning", dates.getOrElse(1) { "Oct 10, 2026" }, "10:00 AM", AppointmentStatus.CONFIRMED, 500),
                Appointment("seed-2", patientName, doctorName, "Dental Checkup", "Sep 12, 2026", "10:00 AM", AppointmentStatus.COMPLETED, 600, "Routine examination completed. Gums are healthy; mild plaque buildup noted.", "Use fluoride toothpaste twice daily. Floss once daily."),
                Appointment("seed-3", patientName, doctorName, "Cavity Filling", "Aug 28, 2026", "11:30 AM", AppointmentStatus.COMPLETED, 1800, "Composite filling completed on lower molar. Bite checked and polished.", "Avoid hard food for 24 hours. Paracetamol if needed."),
                Appointment("seed-4", patientName, doctorName, "Orthodontic Consultation", "Jul 15, 2026", "09:00 AM", AppointmentStatus.COMPLETED, 1000, "Initial braces consultation completed. Digital scan recommended.", "No medication prescribed.")
            )
        )
    }

    private fun seedMessages() {
        messages.addAll(
            listOf(
                ChatMessage("msg-1", Role.DOCTOR, "Hello $patientName, your dental cleaning is scheduled soon. Please arrive 10 minutes early.", "09:12 AM"),
                ChatMessage("msg-2", Role.PATIENT, "Thank you, doctor. Do I need to do anything before the appointment?", "09:18 AM"),
                ChatMessage("msg-3", Role.DOCTOR, "Avoid a heavy meal for about one hour before the visit. Water is fine.", "09:20 AM")
            )
        )
    }

    private fun seedNotices() {
        notices.addAll(
            listOf(
                Notice("notice-1", Role.PATIENT, "Appointment confirmed", "Your upcoming teeth cleaning is confirmed.", "2h ago"),
                Notice("notice-2", Role.PATIENT, "Health tip", "Brush twice daily for two minutes and floss once a day.", "1d ago"),
                Notice("notice-3", Role.DOCTOR, "Today’s schedule", "You have an upcoming confirmed patient appointment.", "1h ago")
            )
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SmileCareTheme { SmileCareApp() } }
    }
}

@Composable
private fun SmileCareTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Teal,
            onPrimary = Color.White,
            secondary = Navy,
            background = Color(0xFFF8FCFD),
            surface = Color.White,
            onSurface = Navy,
            outline = Color(0xFFDCE8EC),
            error = Danger
        ),
        content = content
    )
}

@Composable
private fun SmileCareApp() {
    val context = LocalContext.current
    val store = remember { SmileCareStore(context.applicationContext) }
    var role by remember { mutableStateOf<Role?>(null) }
    var screen by remember { mutableStateOf(AppScreen.AUTH) }
    var selectedAppointmentId by remember { mutableStateOf<String?>(null) }
    var bookingAppointmentId by remember { mutableStateOf<String?>(null) }

    fun goHome(activeRole: Role) {
        screen = if (activeRole == Role.PATIENT) AppScreen.PATIENT_HOME else AppScreen.DOCTOR_HOME
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (screen) {
            AppScreen.AUTH -> AuthScreen(store) { signedRole ->
                role = signedRole
                goHome(signedRole)
            }
            AppScreen.PATIENT_HOME -> PatientHomeScreen(store, navigate = { screen = it }, openAppointment = {
                selectedAppointmentId = it; screen = AppScreen.APPOINTMENT_DETAIL
            })
            AppScreen.PATIENT_BOOK -> BookingScreen(
                store = store,
                existing = store.findAppointment(bookingAppointmentId),
                onBack = { bookingAppointmentId = null; screen = AppScreen.PATIENT_HOME },
                onSaved = { id -> bookingAppointmentId = null; selectedAppointmentId = id; screen = AppScreen.APPOINTMENT_DETAIL }
            )
            AppScreen.PATIENT_APPOINTMENTS -> PatientAppointmentsScreen(store, navigate = { screen = it }, openAppointment = {
                selectedAppointmentId = it; screen = AppScreen.APPOINTMENT_DETAIL
            }, onNew = { bookingAppointmentId = null; screen = AppScreen.PATIENT_BOOK })
            AppScreen.PATIENT_HISTORY -> PatientHistoryScreen(store, navigate = { screen = it }, openAppointment = {
                selectedAppointmentId = it; screen = AppScreen.APPOINTMENT_DETAIL
            })
            AppScreen.PATIENT_PROFILE -> PatientProfileScreen(store, navigate = { screen = it }, onLogout = {
                role = null; screen = AppScreen.AUTH
            })
            AppScreen.PATIENT_NOTIFICATIONS -> NotificationScreen(store, Role.PATIENT, onBack = { screen = AppScreen.PATIENT_HOME })
            AppScreen.PATIENT_DOCTOR -> PatientDoctorScreen(store, onBack = { screen = AppScreen.PATIENT_HOME }, onBook = {
                bookingAppointmentId = null; screen = AppScreen.PATIENT_BOOK
            }, onChat = { screen = AppScreen.CHAT })
            AppScreen.DOCTOR_HOME -> DoctorHomeScreen(store, navigate = { screen = it }, openAppointment = {
                selectedAppointmentId = it; screen = AppScreen.APPOINTMENT_DETAIL
            })
            AppScreen.DOCTOR_APPOINTMENTS -> DoctorAppointmentsScreen(store, navigate = { screen = it }, openAppointment = {
                selectedAppointmentId = it; screen = AppScreen.APPOINTMENT_DETAIL
            })
            AppScreen.DOCTOR_PATIENTS -> DoctorPatientsScreen(store, navigate = { screen = it }, onChat = { screen = AppScreen.CHAT })
            AppScreen.DOCTOR_SCHEDULE -> DoctorScheduleScreen(store, onBack = { screen = AppScreen.DOCTOR_HOME })
            AppScreen.DOCTOR_PROFILE -> DoctorProfileScreen(store, navigate = { screen = it }, onLogout = {
                role = null; screen = AppScreen.AUTH
            })
            AppScreen.DOCTOR_NOTIFICATIONS -> NotificationScreen(store, Role.DOCTOR, onBack = { screen = AppScreen.DOCTOR_HOME })
            AppScreen.APPOINTMENT_DETAIL -> {
                val activeRole = role ?: Role.PATIENT
                AppointmentDetailScreen(
                    role = activeRole,
                    appointment = store.findAppointment(selectedAppointmentId),
                    store = store,
                    onBack = {
                        screen = if (activeRole == Role.PATIENT) AppScreen.PATIENT_APPOINTMENTS else AppScreen.DOCTOR_APPOINTMENTS
                    },
                    onChat = { screen = AppScreen.CHAT },
                    onReschedule = { id -> bookingAppointmentId = id; screen = AppScreen.PATIENT_BOOK }
                )
            }
            AppScreen.CHAT -> {
                val activeRole = role ?: Role.PATIENT
                ChatScreen(store, activeRole, onBack = {
                    screen = if (activeRole == Role.PATIENT) AppScreen.PATIENT_HOME else AppScreen.DOCTOR_HOME
                })
            }
        }
    }
}

@Composable
private fun AuthScreen(store: SmileCareStore, onSignedIn: (Role) -> Unit) {
    var selectedRole by remember { mutableStateOf(Role.PATIENT) }
    var isRegister by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun fillDemo() {
        email = if (selectedRole == Role.PATIENT) "patient@smilecare.app" else "doctor@smilecare.app"
        password = "123456"
        name = if (selectedRole == Role.PATIENT) "Alishba Falaq" else "Dr. Farhana Akter"
        error = null
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))
        Surface(shape = RoundedCornerShape(24.dp), color = Aqua, modifier = Modifier.size(82.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.HealthAndSafety, null, tint = Teal, modifier = Modifier.size(44.dp)) }
        }
        Spacer(Modifier.height(16.dp))
        Text("Smile Care", color = Navy, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
        Text("Dental care for patients and doctors", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(28.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoleCard(Role.PATIENT, selectedRole == Role.PATIENT, "Patient", "Book and manage visits", Icons.Default.Person, Modifier.weight(1f)) {
                selectedRole = Role.PATIENT; error = null; fillDemo()
            }
            RoleCard(Role.DOCTOR, selectedRole == Role.DOCTOR, "Doctor", "Manage clinic workflow", Icons.Default.MedicalServices, Modifier.weight(1f)) {
                selectedRole = Role.DOCTOR; error = null; fillDemo()
            }
        }
        Spacer(Modifier.height(24.dp))

        Text(if (isRegister) "Create ${if (selectedRole == Role.PATIENT) "patient" else "doctor"} account" else "Sign in", color = Navy, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, modifier = Modifier.align(Alignment.Start))
        Text(if (isRegister) "Your account is stored locally on this device for this demo." else "Use your Smile Care account to continue.", color = Muted, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start).padding(top = 4.dp, bottom = 16.dp))

        if (isRegister) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full name") }, leadingIcon = { Icon(Icons.Default.Person, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = true)
            Spacer(Modifier.height(10.dp))
        }
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, leadingIcon = { Icon(Icons.Default.Email, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = true)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = true)
        if (error != null) Text(error!!, color = Danger, fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Spacer(Modifier.height(16.dp))

        PrimaryButton(if (isRegister) "Create account" else "Sign in") {
            error = null
            if (isRegister) {
                when {
                    name.trim().length < 2 -> error = "Enter your full name."
                    !email.contains("@") -> error = "Enter a valid email address."
                    password.length < 6 -> error = "Password must contain at least 6 characters."
                    else -> { store.register(selectedRole, name, email, password); onSignedIn(selectedRole) }
                }
            } else {
                if (store.signIn(selectedRole, email, password)) onSignedIn(selectedRole)
                else error = "Incorrect email or password. Use the demo account below if needed."
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = { fillDemo() }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.Default.AutoFixHigh, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Fill demo ${if (selectedRole == Role.PATIENT) "patient" else "doctor"} account")
        }
        TextButton(onClick = { isRegister = !isRegister; error = null; if (!isRegister) fillDemo() }) {
            Text(if (isRegister) "Already have an account? Sign in" else "New here? Create an account", color = TealDark)
        }
        Spacer(Modifier.height(14.dp))
        Surface(color = SurfaceBlue, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Demo credentials", color = Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Patient: patient@smilecare.app / 123456", color = Muted, fontSize = 11.sp)
                Text("Doctor: doctor@smilecare.app / 123456", color = Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun RoleCard(role: Role, selected: Boolean, title: String, subtitle: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) Aqua else Color.White),
        border = if (selected) BorderStroke(2.dp, Teal) else BorderStroke(1.dp, Color(0xFFE1EAEE))
    ) {
        Column(Modifier.padding(16.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = if (selected) Teal else SurfaceBlue, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = if (selected) Color.White else TealDark) }
            }
            Spacer(Modifier.height(12.dp))
            Text(title, color = Navy, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun PatientHomeScreen(store: SmileCareStore, navigate: (AppScreen) -> Unit, openAppointment: (String) -> Unit) {
    val upcoming = store.appointments.firstOrNull { it.status == AppointmentStatus.CONFIRMED || it.status == AppointmentStatus.PENDING }
    Scaffold(bottomBar = { PatientBottomNav(AppScreen.PATIENT_HOME, navigate) }) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(horizontal = 19.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hello,", color = Muted, fontSize = 13.sp)
                    Text(store.patientName, color = Navy, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Your smile, our priority", color = Muted, fontSize = 12.sp)
                }
                IconButton(onClick = { navigate(AppScreen.PATIENT_NOTIFICATIONS) }) { Icon(Icons.Default.NotificationsNone, null, tint = Navy) }
                HdImage(PATIENT_HD, R.drawable.profile_alishba, Modifier.size(48.dp).clip(CircleShape).clickable { navigate(AppScreen.PATIENT_PROFILE) })
            }
            Spacer(Modifier.height(15.dp))

            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
                Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(22.dp))) {
                    HdImage(TREATMENT_HD, R.drawable.dental_patient, Modifier.fillMaxSize(), ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Navy.copy(alpha = .88f), Teal.copy(alpha = .35f), Color.Transparent))))
                    Column(Modifier.align(Alignment.CenterStart).padding(18.dp).fillMaxWidth(.65f)) {
                        Text("Better oral health", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
                        Text("Book your next dental visit in less than a minute.", color = Color.White.copy(alpha = .9f), fontSize = 12.sp, lineHeight = 16.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { navigate(AppScreen.PATIENT_BOOK) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                            Text("Book now", color = TealDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                QuickAction("Book", Icons.Default.CalendarMonth, SoftGreen) { navigate(AppScreen.PATIENT_BOOK) }
                QuickAction("Dentist", Icons.Default.MedicalServices, SoftPurple) { navigate(AppScreen.PATIENT_DOCTOR) }
                QuickAction("Messages", Icons.Default.ChatBubbleOutline, SoftBlue) { navigate(AppScreen.CHAT) }
                QuickAction("Reports", Icons.Default.Assignment, SoftPink) { navigate(AppScreen.PATIENT_HISTORY) }
            }
            Spacer(Modifier.height(23.dp))

            SectionHeader("Upcoming appointment", if (upcoming != null) "View all" else "Book") {
                if (upcoming != null) navigate(AppScreen.PATIENT_APPOINTMENTS) else navigate(AppScreen.PATIENT_BOOK)
            }
            Spacer(Modifier.height(9.dp))
            if (upcoming != null) AppointmentCard(upcoming, Role.PATIENT) { openAppointment(upcoming.id) }
            else EmptyStateCard("No upcoming appointment", "Book a visit and your next appointment will appear here.", Icons.Default.EventAvailable) { navigate(AppScreen.PATIENT_BOOK) }

            Spacer(Modifier.height(20.dp))
            Text("Popular services", color = Navy, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
            ServiceCard("Teeth Cleaning", "Professional scaling and polishing", "LKR 500", Icons.Default.HealthAndSafety) { navigate(AppScreen.PATIENT_BOOK) }
            ServiceCard("Dental Checkup", "Complete oral examination", "LKR 600", Icons.Default.MedicalInformation) { navigate(AppScreen.PATIENT_BOOK) }
            ServiceCard("Cosmetic Dentistry", "Smile enhancement consultation", "From LKR 1,200", Icons.Default.Star) { navigate(AppScreen.PATIENT_DOCTOR) }

            Spacer(Modifier.height(16.dp))
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.fillMaxWidth().height(132.dp)) {
                    Column(Modifier.weight(1f).padding(16.dp)) {
                        Text("HEALTHY TEETH", color = TealDark, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Confident smile", color = Navy, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Small daily habits make a big difference to oral health.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
                    }
                    HdImage(SMILE_HD, R.drawable.smile_banner, Modifier.width(145.dp).fillMaxHeight(), ContentScale.Crop)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun BookingScreen(store: SmileCareStore, existing: Appointment?, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val services = listOf("Teeth Cleaning", "Dental Checkup", "Cavity Filling", "Orthodontic Consultation", "Cosmetic Dentistry")
    val dates = remember { nextBookingDates() }
    var service by remember(existing?.id) { mutableStateOf(existing?.service ?: services.first()) }
    var date by remember(existing?.id) { mutableStateOf(existing?.date ?: dates.first()) }
    var time by remember(existing?.id) { mutableStateOf(existing?.time ?: store.availability.firstOrNull().orEmpty()) }
    var showConfirm by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)) {
        TopTitle(if (existing == null) "Book Appointment" else "Reschedule", "Choose service, date and time", onBack)
        Spacer(Modifier.height(10.dp))

        Text("Service", color = Navy, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        services.forEach { item ->
            SelectRow(item, service == item, serviceFee(item).let { "LKR $it" }, iconForService(item)) { service = item }
        }
        Spacer(Modifier.height(17.dp))

        Text("Date", color = Navy, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        dates.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                row.forEach { d -> SelectPill(d, date == d, Modifier.weight(1f)) { date = d } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(9.dp))
        }
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Available time slots", color = Navy, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text("Managed by doctor", color = Muted, fontSize = 10.sp)
        }
        Spacer(Modifier.height(8.dp))
        if (store.availability.isEmpty()) {
            Surface(shape = RoundedCornerShape(14.dp), color = SoftOrange, modifier = Modifier.fillMaxWidth()) {
                Text("No time slots are currently available. Please message the doctor.", color = Navy, fontSize = 12.sp, modifier = Modifier.padding(14.dp))
            }
        } else {
            store.availability.toList().chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { slot ->
                        val booked = store.isSlotBooked(date, slot, existing?.id)
                        TimeSlot(slot, selected = time == slot, disabled = booked, Modifier.weight(1f)) { if (!booked) time = slot }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        DoctorSummaryCard(store)
        Spacer(Modifier.height(16.dp))
        PrimaryButton(if (existing == null) "Send booking request" else "Submit new time") {
            if (time.isNotBlank() && !store.isSlotBooked(date, time, existing?.id)) showConfirm = true
        }
        Spacer(Modifier.height(20.dp))
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            icon = { Icon(Icons.Default.CalendarMonth, null, tint = Teal, modifier = Modifier.size(36.dp)) },
            title = { Text(if (existing == null) "Send appointment request?" else "Submit reschedule request?", color = Navy, fontWeight = FontWeight.Bold) },
            text = { Text("$service\n$date at $time\n${store.doctorName}\nFee: LKR ${serviceFee(service)}", color = Muted) },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Back") } },
            confirmButton = {
                Button(onClick = {
                    val id = if (existing == null) store.addAppointment(service, date, time) else {
                        store.rescheduleAppointment(existing.id, service, date, time); existing.id
                    }
                    showConfirm = false
                    onSaved(id)
                }) { Text("Confirm") }
            }
        )
    }
}

@Composable
private fun PatientAppointmentsScreen(store: SmileCareStore, navigate: (AppScreen) -> Unit, openAppointment: (String) -> Unit, onNew: () -> Unit) {
    var filter by remember { mutableStateOf("Upcoming") }
    val list = when (filter) {
        "Upcoming" -> store.appointments.filter { it.status == AppointmentStatus.PENDING || it.status == AppointmentStatus.CONFIRMED }
        "Completed" -> store.appointments.filter { it.status == AppointmentStatus.COMPLETED }
        "Cancelled" -> store.appointments.filter { it.status == AppointmentStatus.CANCELLED }
        else -> store.appointments
    }
    Scaffold(bottomBar = { PatientBottomNav(AppScreen.PATIENT_APPOINTMENTS, navigate) }, floatingActionButton = {
        FloatingActionButton(onClick = onNew, containerColor = Teal, contentColor = Color.White) { Icon(Icons.Default.Add, null) }
    }) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text("Appointments", color = Navy, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            Text("Manage your dental visits", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(15.dp))
            FilterRow(listOf("Upcoming", "Completed", "Cancelled"), filter) { filter = it }
            Spacer(Modifier.height(12.dp))
            if (list.isEmpty()) EmptyStateCard("Nothing here yet", "Your $filter appointments will appear here.", Icons.Default.CalendarMonth, onNew)
            list.forEach { appointment ->
                AppointmentCard(appointment, Role.PATIENT) { openAppointment(appointment.id) }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun PatientHistoryScreen(store: SmileCareStore, navigate: (AppScreen) -> Unit, openAppointment: (String) -> Unit) {
    val history = store.appointments.filter { it.status == AppointmentStatus.COMPLETED || it.status == AppointmentStatus.CANCELLED }
    Scaffold(bottomBar = { PatientBottomNav(AppScreen.PATIENT_HISTORY, navigate) }) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text("Treatment History", color = Navy, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            Text("Reports, prescriptions and previous visits", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            if (history.isEmpty()) EmptyStateCard("No treatment history", "Completed visits and reports will appear here.", Icons.Default.History) { }
            history.forEach { a ->
                AppointmentCard(a, Role.PATIENT) { openAppointment(a.id) }
                if (a.status == AppointmentStatus.COMPLETED && (a.notes.isNotBlank() || a.prescription.isNotBlank())) {
                    Surface(shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp), color = SurfaceBlue, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        Text("Report available • Tap appointment to view", color = TealDark, fontSize = 10.sp, modifier = Modifier.padding(10.dp))
                    }
                }
                Spacer(Modifier.height(11.dp))
            }
        }
    }
}

@Composable
private fun PatientProfileScreen(store: SmileCareStore, navigate: (AppScreen) -> Unit, onLogout: () -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var name by remember(store.patientName) { mutableStateOf(store.patientName) }
    var email by remember(store.patientEmail) { mutableStateOf(store.patientEmail) }
    val completed = store.appointments.count { it.status == AppointmentStatus.COMPLETED }

    Scaffold(bottomBar = { PatientBottomNav(AppScreen.PATIENT_PROFILE, navigate) }) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(horizontal = 19.dp, vertical = 15.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Profile", color = Navy, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(18.dp))
            HdImage(PATIENT_HD, R.drawable.profile_alishba, Modifier.size(96.dp).clip(CircleShape))
            Spacer(Modifier.height(9.dp))
            Text(store.patientName, color = Navy, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Text(store.patientEmail, color = Muted, fontSize = 11.sp)
            Spacer(Modifier.height(17.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat(store.appointments.size.toString(), "Appointments", Modifier.weight(1f))
                MiniStat(completed.toString(), "Treatments", Modifier.weight(1f))
                MiniStat("5.0", "Rating", Modifier.weight(1f))
            }
            Spacer(Modifier.height(18.dp))
            ProfileAction(Icons.Default.Edit, "Personal information", "Edit your name and email") { editing = true }
            ProfileAction(Icons.Default.MedicalInformation, "Medical history", "View completed treatment reports") { navigate(AppScreen.PATIENT_HISTORY) }
            ProfileAction(Icons.Default.NotificationsNone, "Notifications", "Appointments, reminders and updates") { navigate(AppScreen.PATIENT_NOTIFICATIONS) }
            ProfileAction(Icons.Default.ChatBubbleOutline, "Messages", "Chat with ${store.doctorName}") { navigate(AppScreen.CHAT) }
            ProfileAction(Icons.Default.Refresh, "Reset demo data", "Restore sample appointments and messages") { store.resetDemoData() }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("Log out")
            }
        }
    }

    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Edit profile", color = Navy, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true)
                }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } },
            confirmButton = { Button(onClick = { if (name.isNotBlank() && email.contains("@")) { store.updateProfile(Role.PATIENT, name, email); editing = false } }) { Text("Save") } }
        )
    }
}

@Composable
private fun PatientDoctorScreen(store: SmileCareStore, onBack: () -> Unit, onBook: () -> Unit, onChat: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(300.dp)) {
            HdImage(DOCTOR_HD, R.drawable.doctor_farhana, Modifier.fillMaxSize(), ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Navy.copy(alpha = .82f)))))
            IconButton(onClick = onBack, modifier = Modifier.padding(8.dp).background(Color.White.copy(alpha = .86f), CircleShape)) { Icon(Icons.Default.ArrowBack, null, tint = Navy) }
            Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Text("General Dentist", color = Color.White.copy(alpha = .85f), fontSize = 11.sp)
                Text(store.doctorName, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                Text("★ 4.9  •  124 reviews", color = Color.White, fontSize = 12.sp)
            }
        }
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat("5+", "Years", Modifier.weight(1f)); MiniStat("500+", "Patients", Modifier.weight(1f)); MiniStat("98%", "Success", Modifier.weight(1f))
            }
            Spacer(Modifier.height(18.dp))
            Text("Specializations", color = Navy, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpecialtyTile("Cleaning", Icons.Default.HealthAndSafety, Modifier.weight(1f)); SpecialtyTile("Fillings", Icons.Default.MedicalInformation, Modifier.weight(1f)); SpecialtyTile("Cosmetic", Icons.Default.Star, Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            InfoLine(Icons.Default.CreditCard, "Consultation fee", "LKR 500")
            InfoLine(Icons.Default.Schedule, "Working hours", "Sat – Thu  •  9:00 AM – 6:00 PM")
            Spacer(Modifier.height(16.dp))
            Text("About", color = Navy, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("${store.doctorName} provides preventive, restorative and cosmetic dental care with a focus on clear communication and comfortable treatment.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 7.dp))
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedButton(onClick = onChat, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.ChatBubbleOutline, null); Spacer(Modifier.width(6.dp)); Text("Message") }
                Button(onClick = onBook, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Teal)) { Text("Book appointment") }
            }
        }
    }
}

@Composable
private fun NotificationScreen(store: SmileCareStore, role: Role, onBack: () -> Unit) {
    val notices = store.notices.filter { it.target == role }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)) {
        TopTitle("Notifications", "Your latest updates", onBack)
        Spacer(Modifier.height(8.dp))
        if (notices.isEmpty()) EmptyStateCard("You’re all caught up", "New appointment and message updates will appear here.", Icons.Default.NotificationsNone) { }
        notices.forEach { n ->
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Aqua, modifier = Modifier.size(42.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.NotificationsNone, null, tint = TealDark) } }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(n.title, color = Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(n.text, color = Muted, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 3.dp))
                        Text(n.time, color = Color(0xFF9AAAB8), fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorHomeScreen(store: SmileCareStore, navigate: (AppScreen) -> Unit, openAppointment: (String) -> Unit) {
    val pending = store.appointments.filter { it.status == AppointmentStatus.PENDING }
    val active = store.appointments.filter { it.status == AppointmentStatus.CONFIRMED }
    val completed = store.appointments.count { it.status == AppointmentStatus.COMPLETED }
    Scaffold(bottomBar = { DoctorBottomNav(AppScreen.DOCTOR_HOME, navigate) }) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HdImage(DOCTOR_HD, R.drawable.doctor_farhana, Modifier.size(54.dp).clip(CircleShape))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Welcome back,", color = Muted, fontSize = 12.sp)
                    Text(store.doctorName, color = Navy, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    Text("● Online", color = Success, fontSize = 10.sp)
                }
                IconButton(onClick = { navigate(AppScreen.DOCTOR_NOTIFICATIONS) }) { Icon(Icons.Default.NotificationsNone, null, tint = Navy) }
                IconButton(onClick = { navigate(AppScreen.DOCTOR_PROFILE) }) { Icon(Icons.Default.Settings, null, tint = Navy) }
            }
            Spacer(Modifier.height(17.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DoctorStat(active.size.toString(), "Confirmed", SoftGreen, Modifier.weight(1f))
                DoctorStat(pending.size.toString(), "Pending", SoftOrange, Modifier.weight(1f))
                DoctorStat(completed.toString(), "Completed", SoftBlue, Modifier.weight(1f))
            }
            Spacer(Modifier.height(18.dp))

            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Navy)) {
                Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Availability", color = Color.White.copy(alpha = .7f), fontSize = 10.sp)
                        Text("${store.availability.size} active time slots", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("Control which times patients can request.", color = Color.White.copy(alpha = .75f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    Button(onClick = { navigate(AppScreen.DOCTOR_SCHEDULE) }, colors = ButtonDefaults.buttonColors(containerColor = Teal), contentPadding = PaddingValues(horizontal = 14.dp)) { Text("Manage", fontSize = 11.sp) }
                }
            }
            Spacer(Modifier.height(20.dp))

            SectionHeader("Pending requests", if (pending.isNotEmpty()) "See all" else "") { navigate(AppScreen.DOCTOR_APPOINTMENTS) }
            Spacer(Modifier.height(8.dp))
            if (pending.isEmpty()) {
                Surface(shape = RoundedCornerShape(16.dp), color = SoftGreen, modifier = Modifier.fillMaxWidth()) { Text("No appointment requests waiting for approval.", color = Navy, fontSize = 12.sp, modifier = Modifier.padding(14.dp)) }
            } else {
                pending.take(2).forEach { a ->
                    DoctorRequestCard(a, onOpen = { openAppointment(a.id) }, onAccept = { store.updateStatus(a.id, AppointmentStatus.CONFIRMED) }, onDecline = { store.updateStatus(a.id, AppointmentStatus.CANCELLED) })
                    Spacer(Modifier.height(9.dp))
                }
            }

            Spacer(Modifier.height(19.dp))
            SectionHeader("Confirmed schedule", "View all") { navigate(AppScreen.DOCTOR_APPOINTMENTS) }
            Spacer(Modifier.height(8.dp))
            if (active.isEmpty()) EmptyStateCard("No confirmed visits", "Accepted appointments will appear here.", Icons.Default.Schedule) { navigate(AppScreen.DOCTOR_APPOINTMENTS) }
            active.take(3).forEach { a ->
                AppointmentCard(a, Role.DOCTOR) { openAppointment(a.id) }
                Spacer(Modifier.height(9.dp))
            }

            Spacer(Modifier.height(16.dp))
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Aqua), modifier = Modifier.fillMaxWidth().clickable { navigate(AppScreen.CHAT) }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ChatBubbleOutline, null, tint = TealDark, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text("Patient messages", color = Navy, fontWeight = FontWeight.Bold); Text("Open the conversation with ${store.patientName}.", color = Muted, fontSize = 11.sp) }
                    Icon(Icons.Default.ChevronRight, null, tint = TealDark)
                }
            }
        }
    }
}

@Composable
private fun DoctorAppointmentsScreen(store: SmileCareStore, navigate: (AppScreen) -> Unit, openAppointment: (String) -> Unit) {
    var filter by remember { mutableStateOf("All") }
    val options = listOf("All", "Pending", "Confirmed", "Completed")
    val list = store.appointments.filter {
        when (filter) {
            "Pending" -> it.status == AppointmentStatus.PENDING
            "Confirmed" -> it.status == AppointmentStatus.CONFIRMED
            "Completed" -> it.status == AppointmentStatus.COMPLETED
            else -> true
        }
    }
    Scaffold(bottomBar = { DoctorBottomNav(AppScreen.DOCTOR_APPOINTMENTS, navigate) }) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text("Appointments", color = Navy, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            Text("Review requests and manage treatment workflow", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            FilterRow(options, filter) { filter = it }
            Spacer(Modifier.height(12.dp))
            if (list.isEmpty()) EmptyStateCard("No $filter appointments", "Appointments matching this filter will appear here.", Icons.Default.CalendarMonth) { }
            list.forEach { a ->
                AppointmentCard(a, Role.DOCTOR) { openAppointment(a.id) }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun DoctorPatientsScreen(store: SmileCareStore, navigate: (AppScreen) -> Unit, onChat: () -> Unit) {
    val completed = store.appointments.count { it.status == AppointmentStatus.COMPLETED }
    val lastCompleted = store.appointments.firstOrNull { it.status == AppointmentStatus.COMPLETED }
    Scaffold(bottomBar = { DoctorBottomNav(AppScreen.DOCTOR_PATIENTS, navigate) }) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text("Patients", color = Navy, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            Text("Patient records and treatment summary", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HdImage(PATIENT_HD, R.drawable.profile_alishba, Modifier.size(62.dp).clip(CircleShape))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(store.patientName, color = Navy, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text(store.patientEmail, color = Muted, fontSize = 11.sp)
                            Text("$completed completed treatments", color = TealDark, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
                        }
                        IconButton(onClick = onChat) { Icon(Icons.Default.ChatBubbleOutline, null, tint = TealDark) }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 14.dp), color = Color(0xFFE9F0F3))
                    InfoLine(Icons.Default.History, "Latest treatment", lastCompleted?.service ?: "No completed treatment")
                    InfoLine(Icons.Default.MedicalInformation, "Clinical notes", lastCompleted?.notes?.ifBlank { "No notes recorded" } ?: "No notes recorded")
                    InfoLine(Icons.Default.Assignment, "Prescription", lastCompleted?.prescription?.ifBlank { "None" } ?: "None")
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Visit history", color = Navy, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            store.appointments.forEach { a ->
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(a.status)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) { Text(a.service, color = Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text("${a.date} • ${a.time}", color = Muted, fontSize = 10.sp) }
                        StatusBadge(a.status)
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorScheduleScreen(store: SmileCareStore, onBack: () -> Unit) {
    val allSlots = defaultSlots()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)) {
        TopTitle("Availability", "Choose the time slots patients can request", onBack)
        Spacer(Modifier.height(14.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = SoftBlue, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, null, tint = TealDark)
                Spacer(Modifier.width(10.dp))
                Text("Changes apply immediately to the patient booking screen.", color = Navy, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        allSlots.forEach { slot ->
            val enabled = store.availability.contains(slot)
            Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, null, tint = if (enabled) TealDark else Muted)
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) { Text(slot, color = Navy, fontWeight = FontWeight.Bold); Text(if (enabled) "Available to patients" else "Hidden from booking", color = Muted, fontSize = 10.sp) }
                    Switch(checked = enabled, onCheckedChange = { store.toggleAvailability(slot) })
                }
            }
        }
    }
}

@Composable
private fun DoctorProfileScreen(store: SmileCareStore, navigate: (AppScreen) -> Unit, onLogout: () -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var name by remember(store.doctorName) { mutableStateOf(store.doctorName) }
    var email by remember(store.doctorEmail) { mutableStateOf(store.doctorEmail) }
    Scaffold(bottomBar = { DoctorBottomNav(AppScreen.DOCTOR_PROFILE, navigate) }) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(horizontal = 19.dp, vertical = 15.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Doctor Profile", color = Navy, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(17.dp))
            HdImage(DOCTOR_HD, R.drawable.doctor_farhana, Modifier.size(105.dp).clip(CircleShape))
            Spacer(Modifier.height(9.dp))
            Text(store.doctorName, color = Navy, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Text("General Dentist • Online", color = Success, fontSize = 11.sp)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat("5+", "Experience", Modifier.weight(1f)); MiniStat(store.appointments.size.toString(), "Appointments", Modifier.weight(1f)); MiniStat("4.9", "Rating", Modifier.weight(1f))
            }
            Spacer(Modifier.height(18.dp))
            ProfileAction(Icons.Default.Edit, "Professional profile", store.doctorEmail) { editing = true }
            ProfileAction(Icons.Default.Schedule, "Availability", "${store.availability.size} booking slots enabled") { navigate(AppScreen.DOCTOR_SCHEDULE) }
            ProfileAction(Icons.Default.Person, "Patients", "View treatment records") { navigate(AppScreen.DOCTOR_PATIENTS) }
            ProfileAction(Icons.Default.ChatBubbleOutline, "Messages", "Patient conversations") { navigate(AppScreen.CHAT) }
            ProfileAction(Icons.Default.NotificationsNone, "Notifications", "Booking and patient updates") { navigate(AppScreen.DOCTOR_NOTIFICATIONS) }
            ProfileAction(Icons.Default.Refresh, "Reset demo data", "Restore the original sample data") { store.resetDemoData() }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("Log out") }
        }
    }
    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Edit doctor profile", color = Navy, fontWeight = FontWeight.Bold) },
            text = { Column { OutlinedTextField(name, { name = it }, label = { Text("Name") }); Spacer(Modifier.height(8.dp)); OutlinedTextField(email, { email = it }, label = { Text("Email") }) } },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } },
            confirmButton = { Button(onClick = { if (name.isNotBlank() && email.contains("@")) { store.updateProfile(Role.DOCTOR, name, email); editing = false } }) { Text("Save") } }
        )
    }
}

@Composable
private fun AppointmentDetailScreen(
    role: Role,
    appointment: Appointment?,
    store: SmileCareStore,
    onBack: () -> Unit,
    onChat: () -> Unit,
    onReschedule: (String) -> Unit
) {
    if (appointment == null) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            TopTitle("Appointment", null, onBack)
            EmptyStateCard("Appointment not found", "The appointment may have been removed.", Icons.Default.CalendarMonth, onBack)
        }
        return
    }
    var showCancel by remember { mutableStateOf(false) }
    var notes by remember(appointment.id, appointment.notes) { mutableStateOf(appointment.notes) }
    var prescription by remember(appointment.id, appointment.prescription) { mutableStateOf(appointment.prescription) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)) {
        TopTitle("Appointment Details", if (role == Role.PATIENT) "Your dental visit" else "Patient appointment", onBack)
        Spacer(Modifier.height(8.dp))
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(17.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(14.dp), color = Aqua, modifier = Modifier.size(48.dp)) { Box(contentAlignment = Alignment.Center) { Icon(iconForService(appointment.service), null, tint = TealDark) } }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(appointment.service, color = Navy, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold); Text(if (role == Role.PATIENT) appointment.doctor else appointment.patient, color = Muted, fontSize = 11.sp) }
                    StatusBadge(appointment.status)
                }
                HorizontalDivider(Modifier.padding(vertical = 15.dp), color = Color(0xFFE9EFF2))
                DetailLine(Icons.Default.CalendarMonth, "Date", appointment.date)
                DetailLine(Icons.Default.Schedule, "Time", appointment.time)
                DetailLine(Icons.Default.Person, if (role == Role.PATIENT) "Dentist" else "Patient", if (role == Role.PATIENT) appointment.doctor else appointment.patient)
                DetailLine(Icons.Default.CreditCard, "Fee", "LKR ${appointment.fee}")
                DetailLine(Icons.Default.LocationOn, "Clinic", "SmileCare Dental Clinic")
            }
        }
        Spacer(Modifier.height(14.dp))

        if (role == Role.DOCTOR && appointment.status == AppointmentStatus.PENDING) {
            Text("Decision", color = Navy, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedButton(onClick = { showCancel = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Decline", color = Danger) }
                Button(onClick = { store.updateStatus(appointment.id, AppointmentStatus.CONFIRMED) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Teal)) { Icon(Icons.Default.CheckCircle, null); Spacer(Modifier.width(5.dp)); Text("Accept") }
            }
        }

        if (role == Role.DOCTOR && appointment.status == AppointmentStatus.CONFIRMED) {
            Text("Treatment record", color = Navy, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(notes, { notes = it }, label = { Text("Clinical notes") }, modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(15.dp))
            Spacer(Modifier.height(9.dp))
            OutlinedTextField(prescription, { prescription = it }, label = { Text("Prescription / aftercare") }, modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(15.dp))
            Spacer(Modifier.height(10.dp))
            PrimaryButton("Complete treatment") { store.completeAppointment(appointment.id, notes, prescription) }
        }

        if (appointment.status == AppointmentStatus.COMPLETED) {
            Text("Treatment report", color = Navy, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            ReportCard("Clinical notes", appointment.notes.ifBlank { "No clinical notes were recorded." }, Icons.Default.Assignment)
            Spacer(Modifier.height(8.dp))
            ReportCard("Prescription / aftercare", appointment.prescription.ifBlank { "No medication or aftercare instructions were recorded." }, Icons.Default.MedicalInformation)
        }

        Spacer(Modifier.height(14.dp))
        OutlinedButton(onClick = onChat, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp)) { Icon(Icons.Default.ChatBubbleOutline, null); Spacer(Modifier.width(7.dp)); Text(if (role == Role.PATIENT) "Message doctor" else "Message patient") }

        if (role == Role.PATIENT && (appointment.status == AppointmentStatus.PENDING || appointment.status == AppointmentStatus.CONFIRMED)) {
            Spacer(Modifier.height(9.dp))
            Button(onClick = { onReschedule(appointment.id) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = Teal)) { Icon(Icons.Default.CalendarMonth, null); Spacer(Modifier.width(7.dp)); Text("Reschedule") }
            Spacer(Modifier.height(9.dp))
            OutlinedButton(onClick = { showCancel = true }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)) { Icon(Icons.Default.Cancel, null); Spacer(Modifier.width(7.dp)); Text("Cancel appointment") }
        }
        Spacer(Modifier.height(18.dp))
    }

    if (showCancel) {
        AlertDialog(
            onDismissRequest = { showCancel = false },
            title = { Text(if (role == Role.DOCTOR) "Decline appointment?" else "Cancel appointment?", color = Navy, fontWeight = FontWeight.Bold) },
            text = { Text("This will mark the appointment as cancelled. The other side will see the updated status.", color = Muted) },
            dismissButton = { TextButton(onClick = { showCancel = false }) { Text("Keep appointment") } },
            confirmButton = { Button(onClick = { store.updateStatus(appointment.id, AppointmentStatus.CANCELLED); showCancel = false }, colors = ButtonDefaults.buttonColors(containerColor = Danger)) { Text(if (role == Role.DOCTOR) "Decline" else "Cancel") } }
        )
    }
}

@Composable
private fun ChatScreen(store: SmileCareStore, role: Role, onBack: () -> Unit) {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    val otherName = if (role == Role.PATIENT) store.doctorName else store.patientName
    val otherImage = if (role == Role.PATIENT) DOCTOR_HD else PATIENT_HD
    val fallback = if (role == Role.PATIENT) R.drawable.doctor_farhana else R.drawable.profile_alishba

    Scaffold(
        topBar = {
            Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 7.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Navy) }
                HdImage(otherImage, fallback, Modifier.size(43.dp).clip(CircleShape))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { Text(otherName, color = Navy, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text("● Online", color = Success, fontSize = 10.sp) }
                IconButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+94112345678"))) } }) { Icon(Icons.Default.Phone, null, tint = TealDark) }
            }
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth().background(Color.White).padding(10.dp).imePadding(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("Type a message...", fontSize = 12.sp) }, shape = RoundedCornerShape(50))
                Spacer(Modifier.width(8.dp))
                Surface(shape = CircleShape, color = Teal, modifier = Modifier.size(48.dp).clickable { if (input.isNotBlank()) { store.addMessage(role, input); input = "" } }) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Send, null, tint = Color.White) } }
            }
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(horizontal = 13.dp, vertical = 12.dp)) {
            Text("Conversation", color = Muted, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp))
            store.messages.forEach { msg -> ChatBubble(msg, role, otherImage, fallback); Spacer(Modifier.height(9.dp)) }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, viewingAs: Role, otherImage: String, fallback: Int) {
    val mine = message.sender == viewingAs
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Bottom) {
        if (!mine) { HdImage(otherImage, fallback, Modifier.size(30.dp).clip(CircleShape)); Spacer(Modifier.width(7.dp)) }
        Surface(shape = RoundedCornerShape(16.dp), color = if (mine) Teal else SoftBlue, modifier = Modifier.fillMaxWidth(.78f)) {
            Column(Modifier.padding(12.dp)) {
                Text(message.text, color = if (mine) Color.White else Navy, fontSize = 12.sp, lineHeight = 17.sp)
                Text(message.time, color = if (mine) Color.White.copy(alpha = .72f) else Muted, fontSize = 8.sp, modifier = Modifier.align(Alignment.End).padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun PatientBottomNav(current: AppScreen, navigate: (AppScreen) -> Unit) {
    NavigationBar(containerColor = Color.White, tonalElevation = 2.dp) {
        NavigationBarItem(selected = current == AppScreen.PATIENT_HOME, onClick = { navigate(AppScreen.PATIENT_HOME) }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home", fontSize = 9.sp) })
        NavigationBarItem(selected = current == AppScreen.PATIENT_APPOINTMENTS, onClick = { navigate(AppScreen.PATIENT_APPOINTMENTS) }, icon = { Icon(Icons.Default.CalendarMonth, null) }, label = { Text("Appointments", fontSize = 9.sp) })
        NavigationBarItem(selected = current == AppScreen.PATIENT_HISTORY, onClick = { navigate(AppScreen.PATIENT_HISTORY) }, icon = { Icon(Icons.Default.History, null) }, label = { Text("History", fontSize = 9.sp) })
        NavigationBarItem(selected = current == AppScreen.PATIENT_PROFILE, onClick = { navigate(AppScreen.PATIENT_PROFILE) }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Profile", fontSize = 9.sp) })
    }
}

@Composable
private fun DoctorBottomNav(current: AppScreen, navigate: (AppScreen) -> Unit) {
    NavigationBar(containerColor = Color.White, tonalElevation = 2.dp) {
        NavigationBarItem(selected = current == AppScreen.DOCTOR_HOME, onClick = { navigate(AppScreen.DOCTOR_HOME) }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Dashboard", fontSize = 9.sp) })
        NavigationBarItem(selected = current == AppScreen.DOCTOR_APPOINTMENTS, onClick = { navigate(AppScreen.DOCTOR_APPOINTMENTS) }, icon = { Icon(Icons.Default.CalendarMonth, null) }, label = { Text("Appointments", fontSize = 9.sp) })
        NavigationBarItem(selected = current == AppScreen.DOCTOR_PATIENTS, onClick = { navigate(AppScreen.DOCTOR_PATIENTS) }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Patients", fontSize = 9.sp) })
        NavigationBarItem(selected = current == AppScreen.CHAT, onClick = { navigate(AppScreen.CHAT) }, icon = { Icon(Icons.Default.ChatBubbleOutline, null) }, label = { Text("Messages", fontSize = 9.sp) })
        NavigationBarItem(selected = current == AppScreen.DOCTOR_PROFILE, onClick = { navigate(AppScreen.DOCTOR_PROFILE) }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Profile", fontSize = 9.sp) })
    }
}

@Composable
private fun HdImage(url: String, fallbackRes: Int, modifier: Modifier, contentScale: ContentScale = ContentScale.Crop) {
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = painterResource(fallbackRes),
        error = painterResource(fallbackRes)
    )
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = Teal)) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TopTitle(title: String, subtitle: String?, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Navy) }
        Column(Modifier.weight(1f).padding(top = 8.dp)) {
            Text(title, color = Navy, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
            if (subtitle != null) Text(subtitle, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, bg: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(76.dp).clickable(onClick = onClick)) {
        Surface(shape = RoundedCornerShape(15.dp), color = bg, modifier = Modifier.size(54.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = TealDark, modifier = Modifier.size(26.dp)) } }
        Spacer(Modifier.height(6.dp)); Text(label, color = Navy, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SectionHeader(title: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Navy, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (action.isNotBlank()) Text(action, color = TealDark, fontSize = 11.sp, modifier = Modifier.clickable(onClick = onClick))
    }
}

@Composable
private fun ServiceCard(title: String, subtitle: String, price: String, icon: ImageVector, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = Aqua, modifier = Modifier.size(44.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = TealDark) } }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) { Text(title, color = Navy, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(subtitle, color = Muted, fontSize = 10.sp) }
            Text(price, color = TealDark, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AppointmentCard(a: Appointment, role: Role, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(13.dp), color = Aqua, modifier = Modifier.size(45.dp)) { Box(contentAlignment = Alignment.Center) { Icon(iconForService(a.service), null, tint = TealDark, modifier = Modifier.size(22.dp)) } }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.service, color = Navy, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (role == Role.PATIENT) a.doctor else a.patient, color = Muted, fontSize = 10.sp)
                }
                StatusBadge(a.status)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, null, tint = Muted, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(5.dp)); Text(a.date, color = Muted, fontSize = 10.sp)
                Spacer(Modifier.width(12.dp)); Icon(Icons.Default.Schedule, null, tint = Muted, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(5.dp)); Text(a.time, color = Muted, fontSize = 10.sp)
                Spacer(Modifier.weight(1f)); Icon(Icons.Default.ChevronRight, null, tint = Muted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun DoctorRequestCard(a: Appointment, onOpen: () -> Unit, onAccept: () -> Unit, onDecline: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onOpen)) {
                HdImage(PATIENT_HD, R.drawable.profile_alishba, Modifier.size(44.dp).clip(CircleShape))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { Text(a.patient, color = Navy, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text("${a.service} • ${a.date} • ${a.time}", color = Muted, fontSize = 9.sp) }
                StatusBadge(a.status)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Decline", color = Danger, fontSize = 11.sp) }
                Button(onClick = onAccept, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Teal)) { Text("Accept", fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: AppointmentStatus) {
    val pair = when (status) {
        AppointmentStatus.PENDING -> SoftOrange to Warning
        AppointmentStatus.CONFIRMED -> Aqua to TealDark
        AppointmentStatus.COMPLETED -> SoftGreen to Success
        AppointmentStatus.CANCELLED -> SoftPink to Danger
    }
    Surface(shape = RoundedCornerShape(50), color = pair.first) {
        Text(status.name.lowercase().replaceFirstChar { it.uppercase() }, color = pair.second, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
    }
}

@Composable
private fun StatusDot(status: AppointmentStatus) {
    val color = when (status) { AppointmentStatus.PENDING -> Warning; AppointmentStatus.CONFIRMED -> Teal; AppointmentStatus.COMPLETED -> Success; AppointmentStatus.CANCELLED -> Danger }
    Box(Modifier.size(9.dp).clip(CircleShape).background(color))
}

@Composable
private fun EmptyStateCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = SurfaceBlue), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = TealDark, modifier = Modifier.size(34.dp)); Spacer(Modifier.height(8.dp)); Text(title, color = Navy, fontWeight = FontWeight.Bold); Text(subtitle, color = Muted, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun FilterRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { item ->
            Surface(shape = RoundedCornerShape(12.dp), color = if (selected == item) Teal else SurfaceBlue, modifier = Modifier.weight(1f).clickable { onSelect(item) }) {
                Text(item, color = if (selected == item) Color.White else Muted, fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 10.dp), maxLines = 1)
            }
        }
    }
}

@Composable
private fun SelectRow(title: String, selected: Boolean, trailing: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(15.dp), color = if (selected) Aqua else Color.White, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(11.dp), color = if (selected) Teal else SurfaceBlue, modifier = Modifier.size(39.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = if (selected) Color.White else TealDark, modifier = Modifier.size(20.dp)) } }
            Spacer(Modifier.width(10.dp)); Text(title, color = Navy, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 12.sp, modifier = Modifier.weight(1f)); Text(trailing, color = TealDark, fontSize = 10.sp)
        }
    }
}

@Composable
private fun SelectPill(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(13.dp), color = if (selected) Teal else Color.White, modifier = modifier.clickable(onClick = onClick)) {
        Text(label, color = if (selected) Color.White else Navy, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp))
    }
}

@Composable
private fun TimeSlot(label: String, selected: Boolean, disabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg = when { disabled -> Color(0xFFF0F2F3); selected -> Teal; else -> Color.White }
    val fg = when { disabled -> Color(0xFFB3BEC5); selected -> Color.White; else -> Navy }
    Surface(shape = RoundedCornerShape(12.dp), color = bg, modifier = modifier.clickable(enabled = !disabled, onClick = onClick)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 10.dp)) {
            Text(label, color = fg, fontSize = 9.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            if (disabled) Text("Booked", color = fg, fontSize = 7.sp)
        }
    }
}

@Composable
private fun DoctorSummaryCard(store: SmileCareStore) {
    Card(shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            HdImage(DOCTOR_HD, R.drawable.doctor_farhana, Modifier.size(50.dp).clip(CircleShape))
            Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(store.doctorName, color = Navy, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text("General Dentist", color = Muted, fontSize = 10.sp); Text("★ 4.9 • 124 reviews", color = Warning, fontSize = 9.sp) }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(14.dp), color = SurfaceBlue, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 13.dp, horizontal = 4.dp)) { Text(value, color = Navy, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp); Text(label, color = Muted, fontSize = 8.sp, textAlign = TextAlign.Center) }
    }
}

@Composable
private fun DoctorStat(value: String, label: String, bg: Color, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(16.dp), color = bg, modifier = modifier) {
        Column(Modifier.padding(13.dp)) { Text(value, color = Navy, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold); Text(label, color = Muted, fontSize = 9.sp) }
    }
}

@Composable
private fun ProfileAction(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(11.dp), color = Aqua, modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = TealDark, modifier = Modifier.size(21.dp)) } }
            Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(title, color = Navy, fontWeight = FontWeight.SemiBold, fontSize = 12.sp); Text(subtitle, color = Muted, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }; Icon(Icons.Default.ChevronRight, null, tint = Muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SpecialtyTile(label: String, icon: ImageVector, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(15.dp), color = SurfaceBlue, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 14.dp, horizontal = 5.dp)) { Icon(icon, null, tint = TealDark); Spacer(Modifier.height(5.dp)); Text(label, color = Navy, fontSize = 9.sp, textAlign = TextAlign.Center) }
    }
}

@Composable
private fun InfoLine(icon: ImageVector, title: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = TealDark, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(title, color = Muted, fontSize = 9.sp); Text(value, color = Navy, fontSize = 11.sp, lineHeight = 15.sp) }
    }
}

@Composable
private fun DetailLine(icon: ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = TealDark, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp)); Column { Text(title, color = Navy, fontWeight = FontWeight.SemiBold, fontSize = 11.sp); Text(subtitle, color = Muted, fontSize = 10.sp) }
    }
}

@Composable
private fun ReportCard(title: String, text: String, icon: ImageVector) {
    Surface(shape = RoundedCornerShape(16.dp), color = SurfaceBlue, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = TealDark); Spacer(Modifier.width(10.dp)); Column { Text(title, color = Navy, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text(text, color = Muted, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 4.dp)) }
        }
    }
}

private fun iconForService(service: String): ImageVector = when {
    service.contains("Cleaning", true) -> Icons.Default.HealthAndSafety
    service.contains("Checkup", true) -> Icons.Default.MedicalInformation
    service.contains("Cavity", true) -> Icons.Default.MedicalServices
    service.contains("Orthodontic", true) -> Icons.Default.Star
    service.contains("Cosmetic", true) -> Icons.Default.FavoriteBorder
    else -> Icons.Default.MedicalServices
}

private fun serviceFee(service: String): Int = when {
    service.contains("Cleaning", true) -> 500
    service.contains("Checkup", true) -> 600
    service.contains("Cavity", true) -> 1800
    service.contains("Orthodontic", true) -> 1000
    service.contains("Cosmetic", true) -> 1200
    else -> 500
}

private fun defaultSlots(): List<String> = listOf("09:00 AM", "10:00 AM", "11:00 AM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM")

private fun nextBookingDates(): List<String> {
    val format = SimpleDateFormat("EEE, MMM d", Locale.ENGLISH)
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, 1)
    return (0 until 6).map {
        val value = format.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        value
    }
}

private fun currentTime(): String = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date())
