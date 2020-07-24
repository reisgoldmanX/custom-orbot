package org.torproject.android.ui.hiddenservices.dialogs;


import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;

import android.view.View;
import android.widget.Toast;

import org.torproject.android.R;
import org.torproject.android.ui.hiddenservices.backup.BackupUtils;
import org.torproject.android.ui.hiddenservices.permissions.PermissionManager;

public class HSActionsDialog extends DialogFragment {
    public static final int WRITE_EXTERNAL_STORAGE_FROM_ACTION_DIALOG = 2;
    private AlertDialog actionDialog;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle arguments = getArguments();

        final View dialog_view = getActivity().getLayoutInflater().inflate(R.layout.layout_hs_actions, null);
        actionDialog = new AlertDialog.Builder(getActivity())
                .setView(dialog_view)
                .setTitle(R.string.hidden_services)
                .create();

        dialog_view.findViewById(R.id.btn_hs_backup).setOnClickListener(v -> doBackup());

        dialog_view.findViewById(R.id.btn_hs_clipboard).setOnClickListener(v -> {
            Context mContext = v.getContext();
            ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("onion", arguments.getString("onion"));
            clipboard.setPrimaryClip(clip);
            Toast.makeText(mContext, R.string.done, Toast.LENGTH_LONG).show();
            actionDialog.dismiss();
        });

        dialog_view.findViewById(R.id.bt_hs_show_auth).setOnClickListener(v -> {
            String auth_cookie_value = arguments.getString("auth_cookie_value");

            if (arguments.getInt("auth_cookie") == 1) {
                if (auth_cookie_value == null || auth_cookie_value.length() < 1) {
                    Toast.makeText(
                            v.getContext(), R.string.please_restart_Orbot_to_enable_the_changes, Toast.LENGTH_LONG
                    ).show();
                } else {
                    HSCookieDialog dialog = new HSCookieDialog();
                    dialog.setArguments(arguments);
                    dialog.show(getFragmentManager(), "HSCookieDialog");
                }
            } else {
                Toast.makeText(
                        v.getContext(), R.string.auth_cookie_was_not_configured, Toast.LENGTH_LONG
                ).show();
            }

            actionDialog.dismiss();
        });

        dialog_view.findViewById(R.id.btn_hs_delete).setOnClickListener(v -> {
            HSDeleteDialog dialog = new HSDeleteDialog();
            dialog.setArguments(arguments);
            dialog.show(getFragmentManager(), "HSDeleteDialog");
            actionDialog.dismiss();
        });

        dialog_view.findViewById(R.id.btn_hs_cancel).setOnClickListener(v -> actionDialog.dismiss());

        return actionDialog;
    }

    public void doBackup() {
        Context mContext = getActivity();
        if (PermissionManager.isLollipopOrHigher()
                && !PermissionManager.hasExternalWritePermission(getActivity())) {

            PermissionManager.requestExternalWritePermissions(
                    getActivity(), WRITE_EXTERNAL_STORAGE_FROM_ACTION_DIALOG);

            return;
        }

        BackupUtils hsutils = new BackupUtils(mContext);
        String backupPath = hsutils.createZipBackup(Integer.parseInt(getArguments().getString("port")));

        if (backupPath == null || backupPath.length() < 1) {
            Toast.makeText(mContext, R.string.error, Toast.LENGTH_LONG).show();
            actionDialog.dismiss();
            return;
        }

        Toast.makeText(mContext, R.string.backup_saved_at_external_storage, Toast.LENGTH_LONG).show();

        Uri selectedUri = Uri.parse(backupPath.substring(0, backupPath.lastIndexOf("/")));
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(selectedUri, "resource/folder");

        if (intent.resolveActivityInfo(mContext.getPackageManager(), 0) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(mContext, R.string.filemanager_not_available, Toast.LENGTH_LONG).show();
        }
        actionDialog.dismiss();
    }
}
