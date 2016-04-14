package tk.ty3uk.miuilockscreendialer;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class UI extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ui);

        final SharedPreferences pref = getSharedPreferences("dialer", MODE_WORLD_READABLE);
        final SharedPreferences.Editor editor = pref.edit();

        final EditText packageName = (EditText) findViewById(R.id.package_name_edit);
        final EditText packageActivity = (EditText) findViewById(R.id.package_activity_edit);

        try {
            packageName.setText(pref.getString("packageName", "com.android.contacts"));
            packageActivity.setText(pref.getString("packageActivity", "com.android.contacts.activities.TwelveKeyDialer"));

            Button save = (Button) findViewById(R.id.save);
            save.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editor.putString("packageName", packageName.getText().toString());
                    editor.putString("packageActivity", packageActivity.getText().toString());
                    editor.commit();

                    Toast.makeText(getApplicationContext(), R.string.save_success, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

    }
}
