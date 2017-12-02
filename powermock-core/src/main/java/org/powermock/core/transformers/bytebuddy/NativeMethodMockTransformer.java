package org.powermock.core.transformers.bytebuddy;


import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;
import org.powermock.core.transformers.TransformStrategy;
import org.powermock.core.transformers.bytebuddy.advice.MethodDispatcher;
import org.powermock.core.transformers.bytebuddy.advice.MockMethodDispatchers;
import org.powermock.core.transformers.bytebuddy.support.ByteBuddyClass;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.isNative;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static org.powermock.core.transformers.bytebuddy.advice.MockMethodAdvice.VOID;

public class NativeMethodMockTransformer extends AbstractMethodMockTransformer {
    
    public NativeMethodMockTransformer(final TransformStrategy strategy) {
        super(strategy);
        InstanceInception.identifier = getIdentifier();
    }
    
    @Override
    public ByteBuddyClass transform(final ByteBuddyClass clazz) throws Exception {
        final Builder builder = clazz.getBuilder()
                                     .method(isNative().and(not(ElementMatchers.isStatic())))
                                     .intercept(MethodDelegation.to(InstanceInception.class));
        return new ByteBuddyClass(clazz.getTypeDefinitions(), builder);
    }
    
    
    public static class InstanceInception {
        public static String identifier;
    
        @RuntimeType
        public static Object intercept(
                                          @Origin Method origin,
                                          @AllArguments Object[] arguments,
                                          @SuperCall Callable<Object> zuper
        ) throws Throwable {
            final Class<?> returnType = origin.getReturnType();
            final String returnTypeAsString;
            if (!returnType.equals(Void.class)) {
                returnTypeAsString = returnType.getName();
            } else {
                returnTypeAsString = VOID;
            }
            
            final Class<?> mock = origin.getDeclaringClass();
            final MethodDispatcher methodDispatcher = MockMethodDispatchers.get(identifier, mock);
            
            final Callable<Object> execute;
            
            if (methodDispatcher == null) {
                execute = zuper;
            } else {
                final Callable<Object> callable = methodDispatcher.methodCall(mock, origin.getName(), arguments, origin.getParameterTypes(), returnTypeAsString);
                if (callable == null) {
                    execute = zuper;
                }else {
                    execute = callable;
                }
            }
            
            return execute.call();
        }
    }
}
