package com.greatingcard.nammarailubuddy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.greatingcard.nammarailubuddy.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()

        auth.currentUser?.let { user ->
            binding.userEmail.text = user.email
            binding.userName.text = user.displayName ?: "Namma User"
        }

        binding.btnViewApiKeys.setOnClickListener {
            startActivity(Intent(requireContext(), ApiKeysActivity::class.java))
        }

        binding.btnPnrStatus.setOnClickListener {
            startActivity(Intent(requireContext(), PnrStatusActivity::class.java))
        }
        binding.btnTrainSchedule.setOnClickListener {
            startActivity(Intent(requireContext(), TrainScheduleActivity::class.java))
        }
        binding.btnSeatAvailability.setOnClickListener {
            startActivity(Intent(requireContext(), SeatAvailabilityActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            activity?.finish()
        }

        binding.btnAppSettings.setOnClickListener {
            val configured = NammaRailuApp.repository(requireActivity().application).isConfigured
            Toast.makeText(
                context,
                "Namma Railu Buddy v${BuildConfig.VERSION_NAME} • IRCTC API ${if (configured) "configured" else "key missing"}",
                Toast.LENGTH_LONG
            ).show()
        }

        binding.btnPrivacyPolicy.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.indianrail.gov.in")))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
