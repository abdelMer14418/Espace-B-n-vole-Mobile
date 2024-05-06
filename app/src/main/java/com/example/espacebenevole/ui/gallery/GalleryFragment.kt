package com.example.espacebenevole.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.espacebenevole.databinding.FragmentGalleryBinding

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Si vous avez besoin d'ajouter des écouteurs d'événements ou des interactions avec le calendrier
        // Vous pouvez les ajouter ici en utilisant binding.calendarView

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
