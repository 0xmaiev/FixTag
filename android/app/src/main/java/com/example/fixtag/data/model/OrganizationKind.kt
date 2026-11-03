package com.example.fixtag.data.model

import com.google.gson.annotations.SerializedName

enum class OrganizationKind {
    @SerializedName("cliente")
    Cliente,
    @SerializedName("fabricante")
    Fabricante;
}