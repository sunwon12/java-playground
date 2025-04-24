package ex.demo;


import static org.hibernate.sql.ast.SqlTreeCreationLogger.LOGGER;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ArbitraryIntrospectorResult;
import com.navercorp.fixturemonkey.api.introspector.BuilderArbitraryIntrospector;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import com.navercorp.fixturemonkey.api.introspector.FailoverIntrospector;
import com.navercorp.fixturemonkey.api.introspector.FieldReflectionArbitraryIntrospector;
import com.navercorp.fixturemonkey.api.jqwik.ArbitraryUtils;
import com.navercorp.fixturemonkey.api.plugin.SimpleValueJqwikPlugin;
import com.navercorp.fixturemonkey.api.type.TypeCache;
import com.navercorp.fixturemonkey.jakarta.validation.plugin.JakartaValidationPlugin;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.jqwik.api.Arbitraries;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * fixtureMonkey 전역적으로 설정
 * <p>
 * 사용예시:
 * 1.세세한 설정 필요 없을 시 Product product = FixtureMonkeyConfig.fixtureMonkey.giveMeOne(Product.class);
 * <p>
 * 2. 꼭 특정값 지정 필요 시 Product product =
 * FixtureMonkeyConfig.fixtureMonkey.giveMeBuilder(Member.class).set("isDeleted",false).sample();
 */
public class FixtureMonkeyConfig {

    public static final FixtureMonkey fixtureMonkey;

    static {
        fixtureMonkey = FixtureMonkey.builder()
                .objectIntrospector(new FailoverIntrospector(
                        Arrays.asList(
                                FieldReflectionArbitraryIntrospector.INSTANCE,
                                BuilderArbitraryIntrospector.INSTANCE,
                                ConstructorPropertiesArbitraryIntrospector.INSTANCE
                        ), false
                ))
                .plugin(new SimpleValueJqwikPlugin()
                        .minContainerSize(1)
                        .minNumberValue(1))   // 극단적인 값 안나오도록
                .plugin(new JakartaValidationPlugin())  // validation 어노테이션에 따라 객체 생성하도록
                .defaultNotNull(true)                   // null 값 허용 안 하도록
                .pushAssignableTypeContainerPropertyGenerator(
                        List.class,
                        new ListContainerPropertyGenerator() // 리스트 사이즈 3 가지도록
                )
                // id 값 0이상이도록
                .pushAssignableTypeArbitraryIntrospector(
                        Long.class,
                        context -> new ArbitraryIntrospectorResult(
                                ArbitraryUtils.toCombinableArbitrary(
                                        Arbitraries.longs().greaterOrEqual(0L)  // 항상 양수 생성
                                )
                        )
                )
                .pushAssignableTypeArbitraryIntrospector(
                        BigDecimal.class,
                        context -> new ArbitraryIntrospectorResult(
                                ArbitraryUtils.toCombinableArbitrary(
                                        Arbitraries.bigDecimals()
                                                .between(BigDecimal.valueOf(0), BigDecimal.valueOf(200))
                                                .map(bd -> bd.setScale(2, RoundingMode.HALF_UP))
                                )
                        )
                )
                .register(Object.class, fixtureMonkey ->
                        fixtureMonkey.giveMeBuilder(Object.class)
                                .setPostCondition(entity -> {
                                    try {
                                        if (entity == null) { // entity가 null인지 확인
                                            return true;
                                        }

                                        // 현재 엔티티의 모든 필드를 가져옵니다 - TypeCache를 활용하여 성능 개선
                                        Map<String, Field> fields = TypeCache.getFieldsByName(entity.getClass());

                                        // 모든 필드를 순회하면서 양방향 관계를 처리합니다
                                        for (Field field : fields.values()) {
                                            // final이나 static, transient 필드는 처리하지 않습니다
                                            if (Modifier.isFinal(field.getModifiers()) ||
                                                    Modifier.isStatic(field.getModifiers()) ||
                                                    Modifier.isTransient(field.getModifiers())) {
                                                continue;
                                            }
                                            field.setAccessible(true);
                                            Object fieldValue = field.get(entity);

                                            if (field.isAnnotationPresent(OneToMany.class)
                                                    && fieldValue instanceof List<?> list) {
                                                for (Object child : list) {
                                                    // 자식 엔티티에서 부모를 참조하는 필드를 찾습니다
                                                    setParentReference(child, entity);
                                                    setIdNull(child);
                                                }
                                            }

                                            // ManyToOne 관계 처리
                                            if (field.isAnnotationPresent(ManyToOne.class)) {
                                                if (fieldValue != null) {
                                                    // 부모 엔티티에서 자식 컬렉션을 찾아 현재 엔티티를 추가합니다
                                                    setChildrenReference(fieldValue, entity);
                                                }
                                            }

                                            // OneToOne 관계 처리
                                            if (field.isAnnotationPresent(OneToOne.class)) {
                                                if (fieldValue != null) {
                                                    setOneToOneReference(fieldValue, entity);
                                                    setIdNull(fieldValue);
                                                }
                                            }

                                            if (entity instanceof Product) {
                                                Field isDeletedField = entity.getClass().getDeclaredField("soldout");
                                                isDeletedField.setAccessible(true);
                                                isDeletedField.set(entity, false);
                                            }

                                            if (entity instanceof Board) {
                                                Field isDeletedField = entity.getClass().getDeclaredField("isDeleted");
                                                isDeletedField.setAccessible(true);
                                                isDeletedField.set(entity, false);
                                            }

                                        }
                                        return true;
                                    } catch (Exception e) {
                                        LOGGER.warn(
                                                "Bidirectional relationship setup and ID setting failed for entity: {}",
                                                entity.getClass().getName(), e);
                                        return false;
                                    }
                                }))
                // MockMultipartFile 설정
                .pushAssignableTypeArbitraryIntrospector(
                        MultipartFile.class,
                        context -> new ArbitraryIntrospectorResult(
                                ArbitraryUtils.toCombinableArbitrary(
                                        Arbitraries.create(() -> new MockMultipartFile(
                                                "리뷰 이미지",
                                                "testImage.png",
                                                MediaType.IMAGE_PNG_VALUE,
                                                "testImage".getBytes()
                                        ))
                                )
                        )
                )
                .build();
    }

    // 부모 참조를 설정하는 헬퍼 메서드
    private static void setParentReference(Object child, Object parent) throws Exception {
        Map<String, Field> childFields = TypeCache.getFieldsByName(child.getClass());

        for (Field childField : childFields.values()) {
            if (childField.isAnnotationPresent(ManyToOne.class) &&
                    childField.getType().isAssignableFrom(parent.getClass())) {
                childField.setAccessible(true);
                childField.set(child, parent);
                break;
            }
        }
    }

    // 자식 컬렉션에 대한 참조를 설정하는 헬퍼 메서드
    private static void setChildrenReference(Object parent, Object child) throws Exception {
        Map<String, Field> parentFields = TypeCache.getFieldsByName(parent.getClass());

        for (Field parentField : parentFields.values()) {
            if (parentField.isAnnotationPresent(OneToMany.class) &&
                    List.class.isAssignableFrom(parentField.getType())) {
                parentField.setAccessible(true);
                List<Object> children = (List<Object>) parentField.get(parent);
                if (children != null && !children.contains(child)) {
                    children.add(child);
                }
                break;
            }
        }
    }

    // OneToOne 관계를 설정하는 헬퍼 메서드
    private static void setOneToOneReference(Object target, Object source) throws Exception {
        Map<String, Field> targetFields = TypeCache.getFieldsByName(target.getClass());

        for (Field targetField : targetFields.values()) {
            if (targetField.isAnnotationPresent(OneToOne.class) &&
                    targetField.getType().isAssignableFrom(source.getClass())) {
                targetField.setAccessible(true);
                targetField.set(target, source);
                break;
            }
        }
    }

    private static void setIdNull(Object entity) throws Exception {
        Field idField = entity.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, null);
    }
    private static boolean isEntityClass(Class<?> clazz) {
        return clazz.isAnnotationPresent(Entity.class);
    }
}
