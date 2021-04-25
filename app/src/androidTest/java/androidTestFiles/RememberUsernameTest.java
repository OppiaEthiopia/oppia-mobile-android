package androidTestFiles;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.digitalcampus.mobile.learning.R;
import org.digitalcampus.oppia.listener.SubmitEntityListener;
import org.digitalcampus.oppia.model.User;
import org.digitalcampus.oppia.task.RememberUsernameTask;
import org.digitalcampus.oppia.task.ResetPasswordTask;
import org.digitalcampus.oppia.task.result.EntityResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import androidTestFiles.Utils.FileUtils;
import androidTestFiles.org.digitalcampus.oppia.api.MockApiEndpoint;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class RememberUsernameTest {


    private CountDownLatch signal;
    private MockWebServer mockServer;
    private Context context;
    private EntityResult<User> resultUser;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        signal = new CountDownLatch(1);
    }

    @After
    public void tearDown() throws Exception {
        signal.countDown();
        mockServer.shutdown();
    }

    @Test
    public void rememberUsername_SuccessfulResponse() throws Exception {
        try {
            mockServer = new MockWebServer();

            String filename = "responses/response_201_remember_username.json";

            mockServer.enqueue(new MockResponse()
                    .setResponseCode(201)
                    .setBody(FileUtils.getStringFromFile(InstrumentationRegistry.getInstrumentation().getContext(), filename)));

            mockServer.start();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        User user = new User();
        user.setUsername("");
        user.setPassword("");

        try {
            RememberUsernameTask task = new RememberUsernameTask(context, new MockApiEndpoint(mockServer));
            task.setListener(new SubmitEntityListener<User>() {
                @Override
                public void apiKeyInvalidated() {
                }

                @Override
                public void submitComplete(EntityResult<User> result) {
                    resultUser = result;
                    ;
                    signal.countDown();
                }
            });
            task.execute(user);

            signal.await();
            System.out.println(resultUser.getResultMessage());
            assertTrue(resultUser.isSuccess());
            assertEquals(context.getString(R.string.remember_username_complete), resultUser.getResultMessage());

        } catch (InterruptedException ie) {
            ie.printStackTrace();
        } catch (Exception e) {
        }


    }

    @Test
    public void rememberUsername_WrongUsername() throws Exception {
        try {
            mockServer = new MockWebServer();

            String filename = "responses/response_400_remember_username.json";

            mockServer.enqueue(new MockResponse()
                    .setResponseCode(400)
                    .setBody(FileUtils.getStringFromFile(InstrumentationRegistry.getInstrumentation().getContext(), filename)));

            mockServer.start();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        User user = new User();
        user.setUsername("");
        user.setPassword("");

        try {
            RememberUsernameTask task = new RememberUsernameTask(context, new MockApiEndpoint(mockServer));
            task.setListener(new SubmitEntityListener<User>() {
                @Override
                public void apiKeyInvalidated() {
                }

                @Override
                public void submitComplete(EntityResult<User> result) {
                    resultUser = result;
                    ;
                    signal.countDown();
                }
            });
            task.execute(user);

            signal.await();
            System.out.println(resultUser.getResultMessage());
            assertFalse(resultUser.isSuccess());
            assertEquals(context.getString(R.string.error_remember_username), resultUser.getResultMessage());

        } catch (InterruptedException ie) {
            ie.printStackTrace();
        } catch (Exception e) {
        }


    }

    @Test
    public void rememberUsername_EmptyResponse() throws Exception {
        try {
            mockServer = new MockWebServer();

            mockServer.enqueue(new MockResponse()
                    .setBody(""));

            mockServer.start();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        User user = new User();
        user.setUsername("");
        user.setPassword("");

        try {
            RememberUsernameTask task = new RememberUsernameTask(context, new MockApiEndpoint(mockServer));
            task.setListener(new SubmitEntityListener<User>() {
                @Override
                public void apiKeyInvalidated() {
                }

                @Override
                public void submitComplete(EntityResult<User> result) {
                    resultUser = result;
                    ;
                    signal.countDown();
                }
            });
            task.execute(user);

            signal.await();
            System.out.println(resultUser.getResultMessage());
            assertFalse(resultUser.isSuccess());
            assertEquals(context.getString(R.string.error_processing_response), resultUser.getResultMessage());

        } catch (InterruptedException ie) {
            ie.printStackTrace();
        } catch (Exception e) {
        }


    }


}
