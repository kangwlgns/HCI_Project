package com.hci.hci_project

import android.content.Context
import android.widget.Button
import java.lang.Integer.max
import java.lang.Integer.min

class MakingUtility private constructor() {
    companion object {
        fun manClick(
            isMan: Int,
            context: Context,
            buttonStates: Array<Int>,
            buttons: Array<Button>
        ): Int {
            if (isMan == 1) {
                buttons[0].setBackgroundResource(buttonStates[1])
                return 0;
            } else {
                buttons[0].setBackgroundResource(buttonStates[0])
                buttons[1].setBackgroundResource(buttonStates[3])
                return 1
            }
        }

        fun womanClick(
            isMan: Int,
            context: Context,
            buttonStates: Array<Int>,
            buttons: Array<Button>
        ): Int {
            if (isMan == -1) {
                buttons[1].setBackgroundResource(buttonStates[3])
                return 0;
            } else {
                buttons[0].setBackgroundResource(buttonStates[1])
                buttons[1].setBackgroundResource(buttonStates[2])
                return -1
            }
        }

        fun changeClock(cur: String, clock: String): String {
            try {
                var sum: Int = Integer.parseInt(cur) + Integer.parseInt(clock)
                sum = min(sum, 60)
                sum = max(sum, 0)
                return sum.toString()
            } catch (e: Exception) {
                return cur
            }
        }
    }
}