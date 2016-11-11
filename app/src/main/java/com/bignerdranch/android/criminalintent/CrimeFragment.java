package com.bignerdranch.android.criminalintent;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.UUID;

public class CrimeFragment extends Fragment {

    private static final String ARG_CRIME_ID = "crime_id";
    private static final String DIALOG_DATE = "DialogDate";

    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_CONTACT = 1;
    private static final int REQUEST_PHOTO= 2;
    private static final int REQUEST_FACES = 3; //For face tracking view

    private Crime mCrime;
    private File mPhotoFilePrimary;
    private File mPhotoFileSecondary1; //Adding to support the other 3 image views
    private File mPhotoFileSecondary2; //Adding to support the other 3 image views
    private File mPhotoFileSecondary3; //Adding to support the other 3 image views
    private EditText mTitleField;
    private Button mDateButton;
    private CheckBox mSolvedCheckbox;
    private Button mReportButton;
    private Button mSuspectButton;
    private ImageButton mPhotoButton;
    private ImageView mPhotoViewPrimary;
    private ImageView mPhotoViewSecondary1; //Adding to support the other 3 image views
    private ImageView mPhotoViewSecondary2; //Adding to support the other 3 image views
    private ImageView mPhotoViewSecondary3; //Adding to support the other 3 image views
    private int mLastPhoto; //Stores which photo was last taken


    public static CrimeFragment newInstance(UUID crimeId) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_CRIME_ID, crimeId);

        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UUID crimeId = (UUID) getArguments().getSerializable(ARG_CRIME_ID);
        mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);
        mPhotoFilePrimary = CrimeLab.get(getActivity()).getPhotoFile(mCrime, 0);
        mPhotoFileSecondary1 = CrimeLab.get(getActivity()).getPhotoFile(mCrime, 1);
        mPhotoFileSecondary2 = CrimeLab.get(getActivity()).getPhotoFile(mCrime, 2);
        mPhotoFileSecondary3 = CrimeLab.get(getActivity()).getPhotoFile(mCrime, 3);
        mLastPhoto = 3;
    }

    @Override
    public void onPause() {
        super.onPause();

        CrimeLab.get(getActivity())
                .updateCrime(mCrime);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_crime, container, false);

        mTitleField = (EditText) v.findViewById(R.id.crime_title);
        mTitleField.setText(mCrime.getTitle());
        mTitleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCrime.setTitle(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mDateButton = (Button) v.findViewById(R.id.crime_date);
        updateDate();
        mDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager manager = getFragmentManager();
                DatePickerFragment dialog = DatePickerFragment
                        .newInstance(mCrime.getDate());
                dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
                dialog.show(manager, DIALOG_DATE);
            }
        });

        mSolvedCheckbox = (CheckBox) v.findViewById(R.id.crime_solved);
        mSolvedCheckbox.setChecked(mCrime.isSolved());
        mSolvedCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCrime.setSolved(isChecked);
            }
        });

        mReportButton = (Button)v.findViewById(R.id.crime_report);
        mReportButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, getCrimeReport());
                i.putExtra(Intent.EXTRA_SUBJECT,
                        getString(R.string.crime_report_subject));
                i = Intent.createChooser(i, getString(R.string.send_report));

                startActivity(i);
            }
        });

        final Intent pickContact = new Intent(Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_URI);
        mSuspectButton = (Button)v.findViewById(R.id.crime_suspect);
        mSuspectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(pickContact, REQUEST_CONTACT);
            }
        });

        if (mCrime.getSuspect() != null) {
            mSuspectButton.setText(mCrime.getSuspect());
        }

        PackageManager packageManager = getActivity().getPackageManager();
        if (packageManager.resolveActivity(pickContact,
                PackageManager.MATCH_DEFAULT_ONLY) == null) {
            mSuspectButton.setEnabled(false);
        }

        mPhotoButton = (ImageButton) v.findViewById(R.id.crime_camera);
        final Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        boolean canTakePhoto = mPhotoFilePrimary != null &&
                captureImage.resolveActivity(packageManager) != null;
        mPhotoButton.setEnabled(canTakePhoto);

        if (canTakePhoto) {
            Uri uri = Uri.fromFile(mPhotoFilePrimary); //Start with primary slot
            CrimeLab.get(getActivity()).setLastPhoto(mCrime, 3); //Store last taken photo
            captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        }

        mPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //On click, select a new photo slot to fill
                mLastPhoto = (mLastPhoto >= 3) ? 0 : mLastPhoto + 1;
                File photoFile;
                switch (mLastPhoto) //Switch which photo slot to fill
                {
                    case 0:
                        photoFile = mPhotoFilePrimary;
                        break;
                    case 1:
                        photoFile = mPhotoFileSecondary1;
                        break;
                    case 2:
                        photoFile = mPhotoFileSecondary2;
                        break;
                    case 3:
                        photoFile = mPhotoFileSecondary3;
                        break;
                    default:
                        photoFile = mPhotoFilePrimary;
                        break;
                }
                Intent i = new Intent(getContext(), FaceTrackerActivity.class);
                i.putExtra("filename", photoFile.getPath());
                startActivityForResult(i, REQUEST_FACES);
//                Uri uri = Uri.fromFile(photoFile); //Fill selected slot
//                CrimeLab.get(getActivity()).setLastPhoto(mCrime, mLastPhoto); //Store last taken photo
//                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);
//                startActivityForResult(captureImage, REQUEST_PHOTO);
            }
        });

        mPhotoViewPrimary = (ImageView) v.findViewById(R.id.crime_photo);
        mPhotoViewSecondary1 = (ImageView) v.findViewById(R.id.crime_photo_secondary_1); //Initializing new photo views
        mPhotoViewSecondary2 = (ImageView) v.findViewById(R.id.crime_photo_secondary_2); //Initializing new photo views
        mPhotoViewSecondary3 = (ImageView) v.findViewById(R.id.crime_photo_secondary_3); //Initializing new photo views

        updatePhotoView();

        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_DATE) {
            Date date = (Date) data
                    .getSerializableExtra(DatePickerFragment.EXTRA_DATE);
            mCrime.setDate(date);
            updateDate();
        } else if (requestCode == REQUEST_CONTACT && data != null) {
            Uri contactUri = data.getData();
            // Specify which fields you want your query to return
            // values for.
            String[] queryFields = new String[] {
                    ContactsContract.Contacts.DISPLAY_NAME,
            };
            // Perform your query - the contactUri is like a "where"
            // clause here
            ContentResolver resolver = getActivity().getContentResolver();
            Cursor c = resolver
                    .query(contactUri, queryFields, null, null, null);

            try {
                // Double-check that you actually got results
                if (c.getCount() == 0) {
                    return;
                }

                // Pull out the first column of the first row of data -
                // that is your suspect's name.
                c.moveToFirst();

                String suspect = c.getString(0);
                mCrime.setSuspect(suspect);
                mSuspectButton.setText(suspect);
            } finally {
                c.close();
            }
        } else if (requestCode == REQUEST_PHOTO) {
            ContentResolver resolver = getActivity().getContentResolver();
            File photoFile;
            switch (mLastPhoto) //Switch which photo to save to gallery
            {
                case 0:
                    photoFile = mPhotoFilePrimary;
                    break;
                case 1:
                    photoFile = mPhotoFileSecondary1;
                    break;
                case 2:
                    photoFile = mPhotoFileSecondary2;
                    break;
                case 3:
                    photoFile = mPhotoFileSecondary3;
                    break;
                default:
                    photoFile = mPhotoFilePrimary;
                    break;
            }
            try
            {
                MediaStore.Images.Media.insertImage(resolver, photoFile.getPath(), "Crime Photo", "Taken with the CriminalIntent app.");
            } catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
            updatePhotoView();
        } else if (requestCode == REQUEST_FACES)
        {
            String stredittext=data.getStringExtra("faces");
            System.out.println(stredittext);
            updatePhotoView();
        }
    }

    private void updateDate() {
        mDateButton.setText(mCrime.getDate().toString());
    }

    private String getCrimeReport() {
        String solvedString = null;
        if (mCrime.isSolved()) {
            solvedString = getString(R.string.crime_report_solved);
        } else {
            solvedString = getString(R.string.crime_report_unsolved);
        }
        String dateFormat = "EEE, MMM dd";
        String dateString = DateFormat.format(dateFormat, mCrime.getDate()).toString();
        String suspect = mCrime.getSuspect();
        if (suspect == null) {
            suspect = getString(R.string.crime_report_no_suspect);
        } else {
            suspect = getString(R.string.crime_report_suspect, suspect);
        }
        String report = getString(R.string.crime_report,
                mCrime.getTitle(), dateString, solvedString, suspect);
        return report;
    }

    private void updatePhotoView() {
        if (mPhotoFilePrimary == null || !mPhotoFilePrimary.exists()) {
            mPhotoViewPrimary.setImageDrawable(null);
        } else {
            Bitmap bitmap = PictureUtils.getScaledBitmap(
                    mPhotoFilePrimary.getPath(), getActivity());
            mPhotoViewPrimary.setImageBitmap(bitmap);
        }
        if (mPhotoFileSecondary1 == null || !mPhotoFileSecondary1.exists()) { //Draw new photo view
            mPhotoViewSecondary1.setImageDrawable(null);
        } else {
            Bitmap bitmap = PictureUtils.getScaledBitmap(
                    mPhotoFileSecondary1.getPath(), getActivity());
            mPhotoViewSecondary1.setImageBitmap(bitmap);
        }
        if (mPhotoFileSecondary2 == null || !mPhotoFileSecondary2.exists()) { //Draw new photo view
            mPhotoViewSecondary2.setImageDrawable(null);
        } else {
            Bitmap bitmap = PictureUtils.getScaledBitmap(
                    mPhotoFileSecondary2.getPath(), getActivity());
            mPhotoViewSecondary2.setImageBitmap(bitmap);
        }
        if (mPhotoFileSecondary3 == null || !mPhotoFileSecondary3.exists()) { //Draw new photo view
            mPhotoViewSecondary3.setImageDrawable(null);
        } else {
            Bitmap bitmap = PictureUtils.getScaledBitmap(
                    mPhotoFileSecondary3.getPath(), getActivity());
            mPhotoViewSecondary3.setImageBitmap(bitmap);
        }
    }
}
