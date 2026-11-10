package com.callbackdev.chiaro.ui.sky

import androidx.annotation.StringRes
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.sky.Constellation

/**
 * The names of the eighty-eight constellations (Fase 16, step 16.5), by the IAU
 * abbreviation the engine files each one under ([Constellation.id]). Strings and not data,
 * like a star's: the Great Bear is Ursa Major in English, where every chart prints the
 * Latin name, and Orsa Maggiore in Italian, which has a name of its own for each of them.
 * `ConstellationNamesTest` holds the table to the engine's list in both languages.
 */
internal object ConstellationNames {

    /** The name of the constellation with the IAU abbreviation [id], or null when [id] is not one. */
    @StringRes
    fun nameRes(id: String): Int? = names[id]

    @StringRes
    fun nameRes(constellation: Constellation): Int = names.getValue(constellation.id)

    /** Every abbreviation that has a name: the engine's eighty-eight. */
    val ids: Set<String> get() = names.keys

    private val names: Map<String, Int> = mapOf(
        "And" to R.string.constellation_and,
        "Ant" to R.string.constellation_ant,
        "Aps" to R.string.constellation_aps,
        "Aqr" to R.string.constellation_aqr,
        "Aql" to R.string.constellation_aql,
        "Ara" to R.string.constellation_ara,
        "Ari" to R.string.constellation_ari,
        "Aur" to R.string.constellation_aur,
        "Boo" to R.string.constellation_boo,
        "Cae" to R.string.constellation_cae,
        "Cam" to R.string.constellation_cam,
        "Cnc" to R.string.constellation_cnc,
        "CVn" to R.string.constellation_cvn,
        "CMa" to R.string.constellation_cma,
        "CMi" to R.string.constellation_cmi,
        "Cap" to R.string.constellation_cap,
        "Car" to R.string.constellation_car,
        "Cas" to R.string.constellation_cas,
        "Cen" to R.string.constellation_cen,
        "Cep" to R.string.constellation_cep,
        "Cet" to R.string.constellation_cet,
        "Cha" to R.string.constellation_cha,
        "Cir" to R.string.constellation_cir,
        "Col" to R.string.constellation_col,
        "Com" to R.string.constellation_com,
        "CrA" to R.string.constellation_cra,
        "CrB" to R.string.constellation_crb,
        "Crv" to R.string.constellation_crv,
        "Crt" to R.string.constellation_crt,
        "Cru" to R.string.constellation_cru,
        "Cyg" to R.string.constellation_cyg,
        "Del" to R.string.constellation_del,
        "Dor" to R.string.constellation_dor,
        "Dra" to R.string.constellation_dra,
        "Equ" to R.string.constellation_equ,
        "Eri" to R.string.constellation_eri,
        "For" to R.string.constellation_for,
        "Gem" to R.string.constellation_gem,
        "Gru" to R.string.constellation_gru,
        "Her" to R.string.constellation_her,
        "Hor" to R.string.constellation_hor,
        "Hya" to R.string.constellation_hya,
        "Hyi" to R.string.constellation_hyi,
        "Ind" to R.string.constellation_ind,
        "Lac" to R.string.constellation_lac,
        "Leo" to R.string.constellation_leo,
        "LMi" to R.string.constellation_lmi,
        "Lep" to R.string.constellation_lep,
        "Lib" to R.string.constellation_lib,
        "Lup" to R.string.constellation_lup,
        "Lyn" to R.string.constellation_lyn,
        "Lyr" to R.string.constellation_lyr,
        "Men" to R.string.constellation_men,
        "Mic" to R.string.constellation_mic,
        "Mon" to R.string.constellation_mon,
        "Mus" to R.string.constellation_mus,
        "Nor" to R.string.constellation_nor,
        "Oct" to R.string.constellation_oct,
        "Oph" to R.string.constellation_oph,
        "Ori" to R.string.constellation_ori,
        "Pav" to R.string.constellation_pav,
        "Peg" to R.string.constellation_peg,
        "Per" to R.string.constellation_per,
        "Phe" to R.string.constellation_phe,
        "Pic" to R.string.constellation_pic,
        "Psc" to R.string.constellation_psc,
        "PsA" to R.string.constellation_psa,
        "Pup" to R.string.constellation_pup,
        "Pyx" to R.string.constellation_pyx,
        "Ret" to R.string.constellation_ret,
        "Sge" to R.string.constellation_sge,
        "Sgr" to R.string.constellation_sgr,
        "Sco" to R.string.constellation_sco,
        "Scl" to R.string.constellation_scl,
        "Sct" to R.string.constellation_sct,
        "Ser" to R.string.constellation_ser,
        "Sex" to R.string.constellation_sex,
        "Tau" to R.string.constellation_tau,
        "Tel" to R.string.constellation_tel,
        "Tri" to R.string.constellation_tri,
        "TrA" to R.string.constellation_tra,
        "Tuc" to R.string.constellation_tuc,
        "UMa" to R.string.constellation_uma,
        "UMi" to R.string.constellation_umi,
        "Vel" to R.string.constellation_vel,
        "Vir" to R.string.constellation_vir,
        "Vol" to R.string.constellation_vol,
        "Vul" to R.string.constellation_vul
    )
}
