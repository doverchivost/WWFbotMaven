package Singletons;

import Constants.Constants;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.httpclient.HttpTransportClient;

public class Vk {
    private static volatile VkApiClient vk;
    private static volatile GroupActor groupActor;
    private static volatile UserActor userActor;

    public static VkApiClient getVk() {
        VkApiClient localVk = vk;
        if (localVk == null) {
            synchronized (Vk.class) {
                localVk = vk;
                if (localVk == null) {
                    TransportClient transportClient = new HttpTransportClient();
                    vk = localVk = new VkApiClient(transportClient);
                }
            }
        }
        return localVk;
    }

    public static GroupActor getGroupActor() {
        GroupActor localGroupActor = groupActor;
        if (localGroupActor == null) {
            synchronized (Vk.class) {
                localGroupActor = groupActor;
                if (localGroupActor == null) {
                    groupActor = localGroupActor =
                            new GroupActor(Constants.vk_group_id, Constants.vk_group_token);
                }
            }
        }
        return localGroupActor;
    }

    public static UserActor getUserActor() {
        UserActor localUserActor = userActor;
        if (localUserActor == null) {
            synchronized (Vk.class) {
                localUserActor = userActor;
                if (localUserActor == null) {
                    userActor = localUserActor =
                            new UserActor(Constants.vk_admin_id, Constants.vk_admin_token);
                }
            }
        }
        return localUserActor;
    }
}
