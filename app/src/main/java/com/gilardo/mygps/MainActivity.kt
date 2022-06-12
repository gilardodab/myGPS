package com.gilardo.mygps

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.gilardo.mygps.databinding.ActivityMainBinding
import com.gilardo.mygps.model.InternalFileRepository
import com.gilardo.mygps.model.Note
import com.gilardo.mygps.model.NoteRepository
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    //Membuat Variabel untuk menetapkan objek View
    private val repo: NoteRepository by lazy { InternalFileRepository(this) }
    private lateinit var binding: ActivityMainBinding
    private lateinit var locationRequest: LocationRequest
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var datagps: TextView
    private lateinit var datalatitude: TextView
    private lateinit var datalongitude: TextView
    //membuat  variabel untuk API openweathermap
    val CITY : String = "Yogyakarta, ID"
    val API: String = "dc6178b2003d7c9ead2e870ab6b4216f"
    private val PermissionCode = 2

    //menerapkan callback yang aktif saat sistem pertama kali membuat aktivitas
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Membuat objek binding view/Activity
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        /* Buat instance baru dari FusedLocationProviderClient untuk digunakan dalam Activity */
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        //membuat kelas cuaca  dengan 3 parameter untuk menampilkan cuaca dari openwheatermaps
        class weatherTask() : AsyncTask<String, Void, String>() {
            //untuk menginisialisasi dan mengatur UI dasar dari thread utama.
            override fun onPreExecute() {
                super.onPreExecute()
                /*Menampilkan ProgressBar, Membuat desain utama  */
                findViewById<ProgressBar>(R.id.loader).visibility = View.VISIBLE
            }
            //mendeklarasikan jenis variabel nullable dari api open weathermap pada latar belakang
            override fun doInBackground(vararg params: String?): String? {
                var response:String?
                try{
                    response = URL("https://api.openweathermap.org/data/2.5/weather?q=$CITY&units=metric&appid=$API").readText(
                        Charsets.UTF_8
                    )
                }catch (e: Exception){
                    response = null
                }
                return response
            }
            //menambahkan function OnPostExecute
            //metode terakhir dari AsyncTask yang menjalankan setelah penyelesaian tugas dan berjalan di thread UI.
            override fun onPostExecute(result: String?) {
                super.onPostExecute(result)
                try {
                    /* Mengekstrak pengembalian JSON dari API */
                    //membuat variabel yang dibutuhkan pada JSON pada API openweathermaps
                    val jsonObj = JSONObject(result)
                    val main = jsonObj.getJSONObject("main")
                    val sys = jsonObj.getJSONObject("sys")
                    val wind = jsonObj.getJSONObject("wind")
                    val weather = jsonObj.getJSONArray("weather").getJSONObject(0)
                    val updatedAt:Long = jsonObj.getLong("dt")
                    val updatedAtText = ""+ SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(Date(updatedAt*1000))
                    val temp = main.getString("temp")+"°C"
                    val pressure = main.getString("pressure")
                    val humidity = main.getString("humidity")
                    val sunrise:Long = sys.getLong("sunrise")
                    val sunset:Long = sys.getLong("sunset")
                    val windSpeed = wind.getString("speed")
                    val weatherDescription = weather.getString("description")

                    /* Mengisi data yang diekstraksi ke dalam tampilan  */
                    findViewById<TextView>(R.id.updated_at).text =  updatedAtText
                    findViewById<TextView>(R.id.status).text = weatherDescription.capitalize()
                    findViewById<TextView>(R.id.temp).text = temp
                    findViewById<TextView>(R.id.sunrise).text = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunrise*1000))
                    findViewById<TextView>(R.id.sunset).text = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunset*1000))
                    findViewById<TextView>(R.id.wind).text = windSpeed
                    findViewById<TextView>(R.id.pressure).text = pressure
                    findViewById<TextView>(R.id.humidity).text = humidity
                    /* menampilkan loading saat mencari lokasi dan cuaca */
                    findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                } catch (e: Exception) {
                    findViewById<ProgressBar>(R.id.loader).visibility = View.GONE

                }
            }
        }
        //inisialisasi data
        binding.getgps.setOnClickListener {
            checkLocationPermisison()
            weatherTask().execute()
        }
        //memasukan data sensor gps ke file
        binding.log.setOnClickListener {
            var logDataLokasi = binding.editTeksCatatan.text.toString()
            val timeStamp: String = SimpleDateFormat("yy-MM-dd").format(Date())
            binding.editFileName.setText("data_lokasi-" + timeStamp + ".txt")
            val logData1 = binding.alamat.text.toString()
            val logLatitude = binding.Latitude.text.toString()
            val logLongitude = binding.Longitude.text.toString()
            val logstatus = binding.status.text.toString()
            val logsuhu = binding.temp.text.toString()
            logDataLokasi = "$logDataLokasi$logData1 , $logLatitude, $logLongitude, $logstatus $logsuhu\n"
            binding.editTeksCatatan.setText(logDataLokasi)
        }

        //menambahkan binding tombol save
        //untuk menyimpan file ke repo saat tombol Simpan diklik
        binding.Save.setOnClickListener {
            if (binding.editFileName.text.isNotEmpty()) {
                try {
                    repo.addNote(
                        Note(
                            binding.editFileName.text.toString(),
                            binding.editTeksCatatan.text.toString()
                        )
                    )
                } catch (e: Exception) {
                    Toast.makeText(this, "File Write Failed", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
                binding.editFileName.text.clear()
                binding.editTeksCatatan.text.clear()
            } else {
                Toast.makeText(this, "Please provide a Filename", Toast.LENGTH_LONG).show()
            }
        }

        //menambhkan binding read
        //untuk membuka file saat tombol Baca
        binding.Read.setOnClickListener {
            if (binding.editFileName.text.isNotEmpty()) {
                try {
                    val note = repo.getNote(binding.editFileName.text.toString())
                    binding.editTeksCatatan.setText(note.noteText)
                } catch (e: Exception) {
                    Toast.makeText(this, "File Read Failed", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(this, "Please provide a Filename", Toast.LENGTH_LONG).show()
            }
        }
        //menambhakan binding Delete
        //menghapus file beserta isi jika tombol Hapus di klik
        binding.Delete.setOnClickListener {
            if (binding.editFileName.text.isNotEmpty()) {
                try {
                    if (repo.deleteNote(binding.editFileName.text.toString())) {
                        Toast.makeText(this, "File Deleted", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "File Could Not Be Deleted", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "File Delete Failed", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
                binding.editFileName.text.clear()
                binding.editTeksCatatan.text.clear()
            } else {
                Toast.makeText(this, "Please provide a Filename", Toast.LENGTH_LONG).show()
            }
        }

        //membagikan alamat ke sosmed, dll dengan intent saat klik Bagikan
        binding.share.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            val logData1 = binding.alamat.text.toString()
            intent.putExtra(Intent.EXTRA_TEXT, logData1)
            intent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here")
            val chooser = Intent.createChooser(intent, "Bagikan Lokasi Dengan : ")
            startActivity(chooser)
        }
    }

    //mengecek perizinan lokasi hp
    private fun checkLocationPermisison() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            checkGPS()
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100)
        }
    }

    //mengecek GPS hp
    private fun checkGPS() {
        //Buat permintaan lokasi dengan parameter default
        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 2000
        //Membuat variabel Pengaturan akan memeriksa untuk kinerja optimal semua LocationRequests
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)
        val result = LocationServices.getSettingsClient(
            this.applicationContext
        )
            .checkLocationSettings(builder.build())
        result.addOnCompleteListener { task ->
            try {
                //ketika gps on
                val response = task.getResult(
                    ApiException::class.java
                )
                getuserLocation()
            } catch (e: ApiException) {
                //ketika gps off
                e.printStackTrace()
                when (e.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        val resolveApiException = e as ResolvableApiException
                        resolveApiException.startResolutionForResult(this, 200)
                    } catch (sendIntentException: IntentSender.SendIntentException) {
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {

                    }

                }
            }

        }
    }

    //untuk mendapatkan lokasi terkini
    private fun getuserLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // di sini untuk meminta izin yang hilang, dan kemudian menimpa
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // untuk menangani kasus di mana pengguna memberikan izin.
            return
        }
        //mendapatkan lokasi terakhir untuk ditampilkan pada textview
        fusedLocationProviderClient.lastLocation.addOnCompleteListener { task ->

            val location = task.getResult()
            if (location != null) {
                try {
                    datagps = findViewById(R.id.alamat)
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val address = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    val address_line = address[0].getAddressLine(0)

                    //menampilkan alamat geocoder pada textview
                    datagps.text = "" + address_line
                    datalatitude = findViewById(R.id.Latitude)
                    datalongitude = findViewById(R.id.Longitude)
                    //menampilkan data latitude longitude pada textview
                    datalatitude.text = "Latitude : " + location.latitude
                    datalongitude.text = "Longitude : " + location.longitude
                    val address_location = address[0].getAddressLine(0)
                    openLocation(address_location.toString())
                } catch (e: IOException) {
                }
            }
        }
    }
    private fun openLocation(location: String) {
        //membuka lokasi terkini dengan google maps dengan intent
        binding.alamat.setOnClickListener() {
            if (!binding.alamat.text.isEmpty()) {
                val uri = Uri.parse("geo:0, 0?q=$location")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")
                startActivity(intent)
            }
        }
    }
}










