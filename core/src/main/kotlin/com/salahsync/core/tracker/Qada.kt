package com.salahsync.core.tracker

import com.salahsync.core.Prayer
import java.time.LocalDate

/** How a Qada ledger entry changes what is owed. */
enum class QadaKind {
    /** A missed prayer from before tracking began, entered by hand. */
    ADDED,

    /** A missed prayer that has been made up. */
    MADE_UP,
}

data class QadaEntry(val prayer: Prayer, val kind: QadaKind, val count: Int)

/** The Qada picture for one fard prayer. */
data class QadaCount(val missed: Int, val added: Int, val madeUp: Int) {
    val owed: Int get() = (missed + added - madeUp).coerceAtLeast(0)
}

/**
 * Works out Qada from what the user marked. Every fard prayer of a day that has ended,
 * from the day tracking began, counts as missed unless it was marked prayed. Today's
 * prayers are never missed yet, so the user can still tick them late.
 */
object QadaLedger {

    fun missed(trackingSince: LocalDate, today: LocalDate, prayed: Set<Pair<LocalDate, Prayer>>): Map<Prayer, Int> {
        val days = (today.toEpochDay() - trackingSince.toEpochDay()).coerceAtLeast(0).toInt()
        return Prayer.fard.associateWith { p ->
            val done = prayed.count { (day, q) -> q == p && day >= trackingSince && day < today }
            days - done
        }
    }

    fun counts(
        trackingSince: LocalDate,
        today: LocalDate,
        prayed: Set<Pair<LocalDate, Prayer>>,
        entries: List<QadaEntry>,
    ): Map<Prayer, QadaCount> {
        val missed = missed(trackingSince, today, prayed)
        return Prayer.fard.associateWith { p ->
            val mine = entries.filter { it.prayer == p }
            QadaCount(
                missed = missed.getValue(p),
                added = mine.filter { it.kind == QadaKind.ADDED }.sumOf { it.count },
                madeUp = mine.filter { it.kind == QadaKind.MADE_UP }.sumOf { it.count },
            )
        }
    }
}
