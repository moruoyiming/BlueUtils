package com.calypso.bluelib.database;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;


public class StudentRepository {
    private LiveData<List<Student>> liveDataAllStudent;
    private StudentDao studentDao;

    public StudentRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        studentDao = database.studentDao();
        if (liveDataAllStudent == null) {
            liveDataAllStudent = studentDao.getAllLiveDataStudent();
        }
    }

    public void insert(Student... students) {
        studentDao.insert(students);
    }

    public void delete(Student student) {
        studentDao.delete(student);
    }

    public void update(Student student) {
        studentDao.update(student);
    }

    public List<Student> getAll() {
        return studentDao.getAll();
    }

    public LiveData<List<Student>> getLiveDataAllStudent() {
        return studentDao.getAllLiveDataStudent();
    }
}
