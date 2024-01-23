package pl.summernote.summernote.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import pl.summernote.summernote.dataclasses.Collection

class CollectionsViewModel: ViewModel() {
    val collectionsLiveData = MutableLiveData<Collection>()
}