package me.ccrama.redditslide;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.dean.jraw.models.CommentSort;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.meta.SubmissionSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by carlo_000 on 11/19/2015.
 */
public class OfflineSubreddit {

    public static HashMap<String, OfflineSubreddit> subredditBackups = new HashMap<>();
    public long time;
    public ArrayList<Submission> submissions;
    public ArrayList<String> dataNodes;

    public String subreddit;

    public OfflineSubreddit overwriteSubmissions(List<Submission> data) {
        submissions = new ArrayList<>(data);
        subredditBackups.put(subreddit, this);

        return this;
    }

    public void addSubmissions(List<Submission> data) {
        submissions.addAll(data);
        subredditBackups.put(subreddit, this);

    }

    public void writeToMemory() {
        if(subreddit!= null) {
            subredditBackups.put(subreddit, this);
            if (dataNodes == null) {
                StringBuilder s = new StringBuilder();
                s.append(System.currentTimeMillis()).append("<SEPARATOR>");
                for (Submission sub : submissions) {
                    s.append(sub.getDataNode().toString());
                    s.append("<SEPARATOR>");
                }
                String finals = s.toString();
                finals = finals.substring(0, finals.length() - 11);
                Reddit.cachedData.edit().putString(subreddit.toLowerCase(), finals).apply();

            } else {
                StringBuilder s = new StringBuilder();
                s.append(System.currentTimeMillis()).append("<SEPARATOR>");
                for (String sub : dataNodes) {
                    s.append(sub);
                    s.append("<SEPARATOR>");
                }
                String finals = s.toString();
                finals = finals.substring(0, finals.length() - 11);
                Reddit.cachedData.edit().putString(subreddit.toLowerCase(), finals).apply();

                dataNodes = null;
                System.gc();

            }
            System.gc();
        }

    }

    public OfflineSubreddit setCommentAndWrite(String fullname, JsonNode submission, Submission finalSub){
        if(dataNodes == null) {
            dataNodes = new ArrayList<>();
            int index = 0;
            for (Submission s : submissions) {
                if (s.getFullName().equals(fullname)) {
                    dataNodes.add(submission.toString());
                 index = submissions.indexOf(s);

                } else {
                    dataNodes.add(s.getDataNode().toString());
                }
            }
            submissions.remove(index);
            submissions.add(index, finalSub);
        } else {
            int index = 0;
            for (Submission s : submissions) {
                if (s.getFullName().equals(fullname)) {
                    index = submissions.indexOf(s);

                    dataNodes.remove(index);
                    dataNodes.add(index, submission.toString());

                }
            }

            submissions.remove(index);
            submissions.add(index, finalSub);
        }
        return this;
    }

    public OfflineSubreddit(String subreddit) {
        if(subredditBackups.containsKey(subreddit)){
            submissions = subredditBackups.get(subreddit).submissions;
        } else {
            this.subreddit = subreddit;
            String[] split = Reddit.cachedData.getString(subreddit.toLowerCase(), "").split("<SEPARATOR>");
            if (split.length > 1) {
                time = Long.valueOf(split[0]);
                submissions = new ArrayList<>();
                for (int i = 1; i < split.length; i++) {
                    try {
                        if (split[i].startsWith("[")) {
                            submissions.add(SubmissionSerializer.withComments(new ObjectMapper().readTree(split[i]), CommentSort.CONFIDENCE));
                        } else {
                            submissions.add(new Submission(new ObjectMapper().readTree(split[i])));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                submissions = new ArrayList<>();
            }
        }
    }

    public OfflineSubreddit overwriteSubmissions(ArrayList<JsonNode> newSubmissions) {
        StringBuilder s = new StringBuilder();
        s.append(System.currentTimeMillis()).append("<SEPARATOR>");
        for (JsonNode sub : newSubmissions) {
            s.append(sub.toString());
            s.append("<SEPARATOR>");
        }
        String finals = s.toString();
        finals = finals.substring(0, finals.length() - 11);
        Reddit.appRestart.edit().putString(subreddit.toLowerCase() , finals).apply();
        subredditBackups.put(subreddit, this);

        return this;
    }
}
