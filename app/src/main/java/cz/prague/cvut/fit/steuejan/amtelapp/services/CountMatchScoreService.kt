package cz.prague.cvut.fit.steuejan.amtelapp.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cz.prague.cvut.fit.steuejan.amtelapp.R
import cz.prague.cvut.fit.steuejan.amtelapp.business.MatchScoreCounter
import cz.prague.cvut.fit.steuejan.amtelapp.data.entities.Match
import cz.prague.cvut.fit.steuejan.amtelapp.data.entities.Team
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class CountMatchScoreService : Service(), CoroutineScope
{
    companion object
    {
        const val HOME_TEAM = "home"
        const val AWAY_TEAM = "away"
        const val MATCH = "match"
        const val DEFAULT_LOSS = "isDefaultLoss"

        private const val CHANNEL_ID = "countMatchScoreChannel"
        private const val NOTIFICATION_ID = 42
    }

    private val job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    private val notification by lazy {
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_file_upload_blue_24dp)
            .setContentTitle("Nahrávám výsledek zápasu...")
            .setProgress(0, 0, true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        createNotificationChannel()
        if(intent == null)
        {
            val message = "Zkuste prosím zapsat výsledek znovu nebo napište vedoucímu soutěže, pokud již máte vyčerpán limit."

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_error_white_24dp)
                .setContentTitle("Chyba při zápisu utkání")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText(message))
                .setContentText(message)
                .build()

            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
            return START_NOT_STICKY
        }

        val homeTeam = intent.getParcelableExtra<Team>(HOME_TEAM)
        val awayTeam = intent.getParcelableExtra<Team>(AWAY_TEAM)
        val match = intent.getParcelableExtra<Match>(MATCH)
        val defaultLoss = intent.getBooleanExtra(DEFAULT_LOSS, false)

        startForeground(NOTIFICATION_ID, notification)
        launch {
            MatchScoreCounter(
                match,
                homeTeam,
                awayTeam
            )
                .withDefaultLoss(defaultLoss)
                .countTotalScore()
            stopForeground(true)
            stopSelf()
        }

        return START_STICKY
    }

    override fun onDestroy()
    {
        super.onDestroy()
        job.cancel()
    }

    private fun createNotificationChannel()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            val notificationChannel = NotificationChannel(CHANNEL_ID, "Ukládání zápasu", NotificationManager.IMPORTANCE_DEFAULT)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }


    override fun onBind(intent: Intent?) = null
}