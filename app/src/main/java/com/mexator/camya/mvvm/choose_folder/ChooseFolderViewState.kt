package com.mexator.camya.mvvm.choose_folder

data class ChooseFolderViewState(
    val loading: Boolean,
    val dirList: List<String>,
    val currentPath: String,
    val chosenItem: Int?
)