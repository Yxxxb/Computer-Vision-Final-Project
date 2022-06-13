package com.schneewittchen.rosandroid.ui.fragments.ssh;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.schneewittchen.rosandroid.R;
import com.schneewittchen.rosandroid.databinding.FragmentSshBinding;
import com.schneewittchen.rosandroid.viewmodel.SshViewModel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;


/**
 * TODO: Description
 *
 * @author Nils Rottmann
 * @version 1.0.0
 * @created on 18.03.20
 * @updated on 04.06.20
 * @modified by Nils Rottmann
 */

public class SshFragment extends Fragment implements TextView.OnEditorActionListener {

    public static final String TAG = SshFragment.class.getCanonicalName();

    private SshViewModel mViewModel;
    private FragmentSshBinding binding;
    private RecyclerView recyclerView;
    private SshRecyclerViewAdapter mAdapter;
    private AutoCompleteTextView terminalEditText;
    private Button connectButton;
    private FloatingActionButton sendButton;
    private FloatingActionButton abortButton;
    private boolean connected;
    private static final int TCP_SERVER_PORT = 8250;//should be same to the server port
    private static final String host = "192.168.0.108";


    public static SshFragment newInstance() {
        return new SshFragment();
    }

    Runnable runnable1 = new Runnable() {
        @Override
        public void run() {
            runTcpClient1("1");
        }
    };

    Runnable runnable2 = new Runnable() {
        @Override
        public void run() {
            runTcpClient1("2");
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSshBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    private void runTcpClient1(String m) {
        try {
            Socket s = new Socket(host, TCP_SERVER_PORT);//注意host改成你服务器的hostname或IP地址
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
//send output msg
//            String outMsg = "TCP connecting to " + TCP_SERVER_PORT + System.getProperty("line.separator");
            String outMsg = m;
            out.write(outMsg);//发送数据
            out.flush();
            Log.i("TcpClient", "sent: " + outMsg);
//accept server response
            String inMsg = in.readLine() + System.getProperty("line.separator");//得到服务器返回的数据

            Log.i("TcpClient", "received: " + inMsg);
//close connection
            s.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        setConnectionData();
        binding = null;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        // Use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());

        // Specify an adapter
        mAdapter = new SshRecyclerViewAdapter();

        // Define the Recycler View
        recyclerView = view.findViewById(R.id.outputRV);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mAdapter);

        // Get the handles
        terminalEditText = view.findViewById(R.id.terminal_editText);
        connectButton = view.findViewById(R.id.sshConnectButton);
        sendButton = view.findViewById(R.id.sshSendButton);
        abortButton = view.findViewById(R.id.sshAbortButton);

        // Define autocompletion
        String[] autocompletion = getResources().getStringArray(R.array.ssh_autocmpletion);
        Arrays.sort(autocompletion);
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_list_item_1, autocompletion);
        terminalEditText.setAdapter(adapter);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mViewModel = new ViewModelProvider(this).get(SshViewModel.class);

        // Define ViewModel Connection ------
        mViewModel.getSSH().observe(getViewLifecycleOwner(), ssh -> {
            if (ssh == null) return;
            binding.ipAddressEditText.setText(ssh.ip);
            binding.portEditText.setText(Integer.toString(ssh.port));
            binding.usernameEditText.setText(ssh.username);
            binding.passwordEditText.setText(ssh.password);
        });

        // Connect Buttons
        connectButton.setOnClickListener(v -> {
            if (connected) {
                mViewModel.stopSsh();
            } else {
                setConnectionData();
                connectSsh();
            }
        });

        sendButton.setOnClickListener(v -> {
//            final String message = terminalEditText.getText().toString();
////            String message = "cd ~/文档/CV/catkin_ws\nsource install_isolated/setup.bash\nroslaunch cartographer_ros demo_backpack_2d.launch bag_filename:=${HOME}/Downloads/cartographer_paper_deutsches_museum.bag";
//            mViewModel.sendViaSSH(message);
//            terminalEditText.setText("");
//            hideSoftKeyboard();
            new Thread(runnable1).start();
        });

        abortButton.setOnClickListener(v -> {
//            mViewModel.abortAction();
            new Thread(runnable2).start();
        });

        mViewModel.getOutputData().observe(this.getViewLifecycleOwner(), s -> {
            mAdapter.addItem(s);
            recyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
        });

        mViewModel.isConnected().observe(this.getViewLifecycleOwner(), connectionFlag -> {
            connected = connectionFlag;

            if (connectionFlag) {
                connectButton.setText("Disconnect");
            } else {
                connected = false;
                connectButton.setText("Connect");
            }
        });

        // User Input
        binding.ipAddressEditText.setOnEditorActionListener(this);
        binding.portEditText.setOnEditorActionListener(this);
        binding.usernameEditText.setOnEditorActionListener(this);
        binding.passwordEditText.setOnEditorActionListener(this);
    }

    private void connectSsh() {
        mViewModel.connectViaSSH();
    }

    private void hideSoftKeyboard() {
        final InputMethodManager imm = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
    }


    @Override
    public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {

        if (actionId == EditorInfo.IME_ACTION_DONE) {
            setConnectionData();

            view.clearFocus();
            hideSoftKeyboard();

            return true;
        }

        return false;
    }

    public void setConnectionData() {
        Editable sshIp = binding.ipAddressEditText.getText();

        if (sshIp != null) {
            mViewModel.setSshIp(sshIp.toString());
        }

        Editable sshPort = binding.portEditText.getText();

        if (sshPort != null) {
            mViewModel.setSshPort(sshPort.toString());
        }

        Editable sshPassword = binding.passwordEditText.getText();

        if (sshPassword != null) {
            mViewModel.setSshPassword(sshPassword.toString());
        }

        Editable sshUsername = binding.usernameEditText.getText();

        if (sshUsername != null) {
            mViewModel.setSshUsername(sshUsername.toString());
        }
    }

}
