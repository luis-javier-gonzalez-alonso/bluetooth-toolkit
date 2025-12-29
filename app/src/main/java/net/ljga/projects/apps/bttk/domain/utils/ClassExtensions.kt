package net.ljga.projects.apps.bttk.domain.utils

import android.bluetooth.BluetoothGattCharacteristic
import java.util.UUID

fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean =
    this.properties and property != 0

fun BluetoothGattCharacteristic.prettyName(): String = this.uuid.prettyCharacteristicName()

fun UUID.prettyCharacteristicName(): String = this.toString().prettyCharacteristicName()

fun String.prettyCharacteristicName(): String =
    "0x" + this.take(8).trimStart('0')