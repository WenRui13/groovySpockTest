package com.jd.pop.qa;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @FunctionDesc
 * @Author bjyfxuxiaojun
 * @CreateDate 2017/8/1
 * @Reviewer kongxiangyun
 * @ReviewDate 2017/8/1
 */
public class FileRwTool {
    public static List<String> getVenderFromCsv(String csvName) throws IOException {
        File file = new File(System.getProperty("user.dir") + File.separator + csvName);
        HashSet<String> venderSet = new HashSet<String>();

        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        bufferedReader.readLine();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            venderSet.add(line);
        }
        bufferedReader.close();

        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
        bufferedWriter.write("venderId");
        bufferedWriter.newLine();
        for (String venderId : venderSet) {
            bufferedWriter.write(venderId);
            bufferedWriter.newLine();
        }
        bufferedWriter.flush();
        bufferedWriter.close();

        return new ArrayList<String>(venderSet);

    }


    public static void writeValidVender2Csv(String csvName, List<String> venderList) throws IOException {
        File file = new File(System.getProperty("user.dir") + File.separator + csvName);
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
        bufferedWriter.write("venderId");
        bufferedWriter.newLine();
        for (String venderId : venderList) {
            bufferedWriter.write(venderId);
            bufferedWriter.newLine();
        }
        bufferedWriter.flush();
        bufferedWriter.close();
    }


    public static Set<String> readFromFile(String fileName) throws IOException {
        Set<String> set = new HashSet<String>();
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String line;
        while ((line = br.readLine()) != null) {
            set.add(line.trim());
        }
        br.close();
        return set;
    }

    public static String requestCookieValue(String fileName) throws IOException {
        String absoluteFilePath = System.getProperty("user.dir") + File.separator + fileName;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(absoluteFilePath));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }
        bufferedReader.close();
        return stringBuilder.toString();
    }
}
