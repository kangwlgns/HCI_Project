package com.hci.hci_project

import android.content.Context
import android.widget.Button

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
    }
}