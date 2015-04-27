package org.elasticsearch.plugin.analysis.ik;

import org.elasticsearch.common.inject.Module;
import org.elasticsearch.index.analysis.AnalysisModule;
import org.elasticsearch.index.analysis.IkAnalysisBinderProcessor;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;

import com.hichao.elasticsearch.rest.IkReloadDictionaryRestAction;


public class AnalysisIkPlugin extends AbstractPlugin {

    @Override public String name() {
        return "analysis-ik";
    }


    @Override public String description() {
        return "ik analysis";
    }


    @Override public void processModule(Module module) {
        if (module instanceof AnalysisModule) {
            AnalysisModule analysisModule = (AnalysisModule) module;
            analysisModule.addProcessor(new IkAnalysisBinderProcessor());
        }
        
        if (module instanceof RestModule) {
            ((RestModule) module).addRestAction(IkReloadDictionaryRestAction.class);
        }
    }
}
